package com.umkt.ucopy.ui.state

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.umkt.ucopy.ui.widget.NoteWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.R
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.model.Task
import com.umkt.ucopy.utils.BackupHelper
import com.umkt.ucopy.utils.ExportImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.umkt.ucopy.ui.common.ViewDensity

class ClipViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClipRepository(application)
    private val prefs = UserPreferences(getApplication())
    private val json = Json { ignoreUnknownKeys = true }
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var allItems = listOf<Clip>()
    private val _deletedItems = MutableStateFlow<List<DeletedItem>>(emptyList())
    val deletedItems: StateFlow<List<DeletedItem>> = _deletedItems.asStateFlow()

    private val _viewDensity = MutableStateFlow(
        try {
            ViewDensity.valueOf(prefs.getString("view_density", ViewDensity.COMFORTABLE.name))
        } catch (e: Exception) { ViewDensity.COMFORTABLE }
    )
    val viewDensity: StateFlow<ViewDensity> = _viewDensity.asStateFlow()

    private var _categories = MutableStateFlow<List<String>>(emptyList())
    private val customLabelsFlow = MutableStateFlow<Set<String>>(emptySet())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val backupMutex = Mutex()
    private var autoBackupJob: kotlinx.coroutines.Job? = null

    private val _autoBackupFileUri = MutableStateFlow<String?>(null)
    val autoBackupFileUri: StateFlow<String?> = _autoBackupFileUri.asStateFlow()

    private val _autoBackupFileName = MutableStateFlow<String>("auto_backup.json")
    val autoBackupFileName: StateFlow<String> = _autoBackupFileName.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _autoDeleteDays = MutableStateFlow(0)
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    
    private val LARGE_NOTE_ACK_KEY = "large_note_acknowledged"

    init {
        // Load from SharedPreferences cache SYNCHRONOUSLY for ZERO flash
        try {
            val cacheFile = java.io.File(getApplication<Application>().filesDir, "clips_cache.json")
            val cachedJson = if (cacheFile.exists()) cacheFile.readText() else null
            if (cachedJson != null) {
                allItems = json.decodeFromString(cachedJson)
                _uiState.value = _uiState.value.copy(
                    items = allItems,
                    isLoading = false,
                    hasCompletedInitialLoad = true
                )
            }
        } catch (e: Exception) {}


        val sortName = prefs.getString("sort_option", SortOption.AZ.name)
        val sortOption = try {
            SortOption.valueOf(sortName)
        } catch (e: Exception) {
            SortOption.AZ
        }
        _uiState.value = _uiState.value.copy(sortOption = sortOption)

        _autoBackupFileUri.value = prefs.getStringOrNull("auto_backup_file_uri")
        _autoBackupFileName.value = prefs.getString("auto_backup_file_name", "auto_backup.json")
        _autoBackupEnabled.value = try {
            prefs.getBoolean("auto_backup_enabled", false)
        } catch (e: ClassCastException) {
            prefs.getString("auto_backup_enabled", "false").toBoolean()
        }

        _autoBackupFileUri.value?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val context = getApplication<Application>()
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { }
        }

        _autoDeleteDays.value = prefs.getInt("recycle_bin_auto_delete_days", 0)

        customLabelsFlow.value = prefs.getStringSet("custom_labels", emptySet())

        try {
            val recycleFile = java.io.File(getApplication<Application>().filesDir, "recycle_bin.json")
            if (recycleFile.exists()) {
                _deletedItems.value = json.decodeFromString(recycleFile.readText())
            }
        } catch (_: Exception) { }

        // Automatically clean up old items on app start
        cleanOldDeletedItems(_autoDeleteDays.value)

        loadItems()
    }

    fun isLargeNoteAcknowledged(noteId: String): Boolean {
        val ackSet = prefs.getStringSet(LARGE_NOTE_ACK_KEY, emptySet())
        return ackSet.contains(noteId)
    }

    fun acknowledgeLargeNote(noteId: String) {
        val ackSet = prefs.getStringSet(LARGE_NOTE_ACK_KEY, emptySet()).toMutableSet()
        ackSet.add(noteId)
        prefs.putStringSet(LARGE_NOTE_ACK_KEY, ackSet)
    }

    fun acknowledgeMultipleNotes(noteIds: List<String>) {
        val ackSet = prefs.getStringSet(LARGE_NOTE_ACK_KEY, emptySet()).toMutableSet()
        ackSet.addAll(noteIds)
        prefs.putStringSet(LARGE_NOTE_ACK_KEY, ackSet)
    }

    fun acknowledgeAllCurrentNotes() {
        val ids = allItems.map { it.id }
        acknowledgeMultipleNotes(ids)
    }

    fun loadItems() {
        viewModelScope.launch {
            // RULE 4: Only show loading if we have NO cache
            if (!_uiState.value.hasCompletedInitialLoad) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                allItems = repository.getAll()
                _uiState.value = _uiState.value.copy(
                    items = allItems,
                    isLoading = false,
                    hasCompletedInitialLoad = true
                )
                applyFilterAndSort()
                loadLabels()
                updateWidgets()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasCompletedInitialLoad = true,
                    snackbarMessage = getApplication<Application>().getString(R.string.msg_load_failed, e.message ?: "")
                )
            }
        }
    }

    private fun saveRecycleBin() {
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "recycle_bin.json")
            file.writeText(json.encodeToString(_deletedItems.value))
        } catch (_: Exception) { }
    }

    @Suppress("DEPRECATION")
    private fun updateWidgets() {
        val context = getApplication<Application>()
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val intent = android.content.Intent(context, NoteWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
            // Force the list inside the widget to reload
            for (id in ids) {
                manager.notifyAppWidgetViewDataChanged(intArrayOf(id), com.umkt.ucopy.R.id.widget_list)
            }
        }
    }

    fun loadLabels() {
        viewModelScope.launch {
            val fromNotes = repository.getAllCategories()
            val combined = (fromNotes + customLabelsFlow.value).distinct().sorted()
            _categories.value = combined
        }
    }
    
    fun addLabel(name: String) {
        if (name.isBlank()) return
        val updated = customLabelsFlow.value + name.trim()
        customLabelsFlow.value = updated
        prefs.putStringSet("custom_labels", updated)
        loadLabels()
    }

    fun renameLabel(oldName: String, newName: String) {
        viewModelScope.launch {
            val updatedItems = allItems.map { item ->
                if (item.category?.split(", ")?.contains(oldName) == true) {
                    val newList = item.category.split(", ").map { if (it == oldName) newName else it }.distinct()
                    item.copy(category = newList.joinToString(", "))
                } else item
            }
            updatedItems.forEach { repository.update(it) }
            allItems = updatedItems
            
            // Update prefs for custom labels
            val updatedLabels = customLabelsFlow.value.map { if (it == oldName) newName.trim() else it }.toSet()
            customLabelsFlow.value = updatedLabels
            prefs.putStringSet("custom_labels", updatedLabels)
            
            loadLabels()
            applyFilterAndSort()
            performAutoBackup()
        }
    }

    fun deleteLabel(name: String) {
        viewModelScope.launch {
            val updatedItems = allItems.map { item ->
                if (item.category?.split(", ")?.contains(name) == true) {
                    val newList = item.category.split(", ").filter { it != name }
                    item.copy(category = if (newList.isEmpty()) null else newList.joinToString(", "))
                } else item
            }
            updatedItems.forEach { repository.update(it) }
            allItems = updatedItems
            
            // Update prefs to remove custom label
            val updatedLabels = customLabelsFlow.value - name
            customLabelsFlow.value = updatedLabels
            prefs.putStringSet("custom_labels", updatedLabels)
            
            loadLabels()
            applyFilterAndSort()
            performAutoBackup()
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(120)
            applyFilterAndSort()
        }
    }

    fun setCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilterAndSort()
    }


    fun setSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        prefs.putString("sort_option", option.name)
        applyFilterAndSort()
        performAutoBackup()
    }

    fun toggleFavoritesFilter() {
    }

    private fun applyFilterAndSort() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val category = _uiState.value.selectedCategory

        var filtered = allItems

        if (q.isNotEmpty()) {
            val regex = try {
                Regex(Regex.escape(q), RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                null
            }
            filtered = if (regex != null) {
                filtered.filter { regex.containsMatchIn(it.title) || regex.containsMatchIn(it.content) }
            } else {
                filtered.filter { it.title.contains(q, true) || it.content.contains(q, true) }
            }
        }


        category?.let { cat ->
            filtered = filtered.filter { it.category?.split(", ")?.contains(cat) == true }
        }


        val baseSorted = when (_uiState.value.sortOption) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> filtered.sortedBy { it.timestamp }
            SortOption.AZ -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ZA -> filtered.sortedByDescending { it.title.lowercase() }
        }
        
        // Stable sort ensures Pinned items stay at the top, while preserving the chosen sort order
        val sorted = baseSorted.sortedByDescending { it.isPinned }

        _uiState.value = _uiState.value.copy(items = sorted)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cached = json.encodeToString(allItems)
                java.io.File(getApplication<Application>().filesDir, "clips_cache.json").writeText(cached)
            } catch (e: Exception) {}
        }
    }

    fun addOrUpdateItem(
        title: String,
        content: String,
        existingId: String? = null,
        category: String? = null,
        todoItems: List<Task> = emptyList(),
        isTodo: Boolean = false,
        showSnackbar: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (!existingId.isNullOrBlank()) {
                    val existing = allItems.find { it.id == existingId }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = title,
                            content = content,
                            category = category,
                            todoItems = todoItems,
                            isTodo = isTodo
                        )
                        repository.update(updated)
                        allItems = allItems.map { if (it.id == existingId) updated else it }
                    } else {
                        val newItem = Clip(
                            id = existingId,
                            title = title,
                            content = content,
                            category = category,
                            todoItems = todoItems,
                            isTodo = isTodo
                        )
                        repository.add(newItem)
                        allItems = listOf(newItem) + allItems
                    }
                } else {
                    val item = Clip(
                        title = title,
                        content = content,
                        category = category,
                        todoItems = todoItems,
                        isTodo = isTodo
                    )
                    repository.add(item)
                    allItems = listOf(item) + allItems
                }
                applyFilterAndSort()
                loadLabels()
                performAutoBackup()
                updateWidgets()
                if (showSnackbar) {
                    _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_item_saved))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_error_generic, e.message ?: ""))
            }
        }
    }

    fun addItemDirectly(item: Clip) {
        allItems = listOf(item) + allItems
        applyFilterAndSort()
        loadLabels()
        performAutoBackup()
    }

    suspend fun clearAllPermanently() {
        try {
            val ids = allItems.map { it.id }
            ids.forEach { id ->
                repository.delete(id)
            }
            allItems = emptyList()
            applyFilterAndSort()
            loadLabels()
            performAutoBackup()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_clearing_failed, e.message ?: ""))
        }
    }

    suspend fun addAllItems(items: List<Clip>) {
        try {
            items.forEach { item ->
                repository.add(item)
            }
            val newIds = items.map { it.id }.toSet()
            allItems = items + allItems.filter { it.id !in newIds }
            applyFilterAndSort()
            loadLabels()
            performAutoBackup()
            _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_items_added, items.size))
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_adding_failed, e.message ?: ""))
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                val item = allItems.find { it.id == id }
                item?.let {
                    val current = _deletedItems.value.toMutableList()
                    current.add(0, DeletedItem(it))
                    _deletedItems.value = current
                    saveRecycleBin()
                }
                repository.delete(id)
                allItems = allItems.filter { it.id != id }
                applyFilterAndSort()
                loadLabels()
                performAutoBackup()
                updateWidgets()
                _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_item_deleted))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }

    fun deleteSelected(ids: List<String>) {
        viewModelScope.launch {
            try {
                ids.forEach { id ->
                    val item = allItems.find { it.id == id }
                    item?.let {
                        val current = _deletedItems.value.toMutableList()
                        current.add(0, DeletedItem(it))
                        _deletedItems.value = current
                    }
                    repository.delete(id)
                }
                saveRecycleBin()
                allItems = allItems.filter { it.id !in ids }
                applyFilterAndSort()
                loadLabels()
                performAutoBackup()
                updateWidgets()
                _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_items_deleted, ids.size))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }

    fun restoreItem(id: String) {
        viewModelScope.launch {
            val current = _deletedItems.value.toMutableList()
            val deletedItem = current.find { it.item.id == id }
            deletedItem?.let {
                try {
                    repository.add(it.item)
                    allItems = listOf(it.item) + allItems
                    current.remove(it)
                    _deletedItems.value = current
                    saveRecycleBin()
                    applyFilterAndSort()
                    loadLabels()
                    performAutoBackup()
                    _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_item_restored))
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_restore_failed, e.message ?: ""))
                }
            }
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            val itemsToRestore = _deletedItems.value.map { it.item }
            if (itemsToRestore.isEmpty()) return@launch
            try {
                itemsToRestore.forEach { item ->
                    repository.add(item)
                }
                allItems = itemsToRestore + allItems
                _deletedItems.value = emptyList()
                saveRecycleBin()
                applyFilterAndSort()
                loadLabels()
                performAutoBackup()
                _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_items_restored, itemsToRestore.size))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.msg_restore_all_failed, e.message ?: ""))
            }
        }
    }

    fun clearDeletedItems() {
        _deletedItems.value = emptyList()
        saveRecycleBin()
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            try {
                val item = allItems.find { it.id == id }
                item?.let {
                    val updated = it.copy(isPinned = !it.isPinned)
                    repository.update(updated)
                    allItems = allItems.map { if (it.id == id) updated else it }
                    applyFilterAndSort()
                    performAutoBackup()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }


    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("U Copy", text)
        clipboard.setPrimaryClip(clip)
        _uiState.value = _uiState.value.copy(snackbarMessage = getApplication<Application>().getString(R.string.copied_to_clipboard))
    }

    fun shareText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_via)))
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun setViewDensity(density: ViewDensity) {
        _viewDensity.value = density
        saveViewDensityPreference()
        performAutoBackup()
    }

    private fun loadViewDensityPreference() {
        val densityName = prefs.getString("view_density", ViewDensity.COMFORTABLE.name)
        val migrated = when (densityName) {
            "COZY" -> ViewDensity.COMFORTABLE.name
            "SPACIOUS" -> ViewDensity.DETAILED.name
            else -> densityName
        }
        _viewDensity.value = try {
            ViewDensity.valueOf(migrated)
        } catch (e: IllegalArgumentException) {
            ViewDensity.COMFORTABLE
        }
    }

    private fun saveViewDensityPreference() {
        prefs.putString("view_density", _viewDensity.value.name)
    }

    private fun getAutoBackupFileUri(): String? {
        return prefs.getStringOrNull("auto_backup_file_uri")
    }

    private fun saveAutoBackupFileUri(uri: String) {
        prefs.putString("auto_backup_file_uri", uri)
        _autoBackupFileUri.value = uri
    }

    private fun takePersistablePermission(uri: Uri) {
        try {
            val context = getApplication<Application>()
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) { }
    }

    fun setAutoBackupEnabled(enabled: Boolean, fileUri: String? = null, fileName: String? = null) {
        prefs.putBoolean("auto_backup_enabled", enabled)
        _autoBackupEnabled.value = enabled

        if (enabled && fileUri != null) {
            saveAutoBackupFileUri(fileUri)
            if (fileName != null) {
                prefs.putString("auto_backup_file_name", fileName)
                _autoBackupFileName.value = fileName
            }
            _autoBackupFileUri.value = fileUri
            takePersistablePermission(Uri.parse(fileUri))
            performAutoBackup()
        }
    }

    fun setAutoBackupFile(uri: String, fileName: String) {
        saveAutoBackupFileUri(uri)
        prefs.putString("auto_backup_file_name", fileName)
        _autoBackupFileUri.value = uri
        _autoBackupFileName.value = fileName
        takePersistablePermission(Uri.parse(uri))
        if (_autoBackupEnabled.value) {
            performAutoBackup()
        }
    }

    fun hasAutoBackupFile(): Boolean = _autoBackupFileUri.value != null

    fun performAutoBackup() {
        if (!_autoBackupEnabled.value) return
        val uriString = getAutoBackupFileUri()
        if (uriString == null) return
        
        // Debounce: Cancel the previous job and wait 2 seconds before saving
        // This prevents the app from freezing if the user types very fast
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            backupMutex.withLock {
                try {
                    val uri = Uri.parse(uriString)
                    val clips = allItems
                    BackupHelper.exportBackupAsJson(getApplication(), uri, clips, prefs)
                } catch (e: Exception) { }
            }
        }
    }

    fun getAutoBackupState(): Boolean = _autoBackupEnabled.value

    fun getAutoBackupFileName(): String = _autoBackupFileName.value

    fun getAllItems(): List<Clip> = allItems

    fun onAppResumed() {
        cleanOldDeletedItems(_autoDeleteDays.value)
    }

    fun setAutoDeleteDays(days: Int) {
        _autoDeleteDays.value = days
        prefs.putInt("recycle_bin_auto_delete_days", days)
        
        // Reset countdown for existing items so they don't get instantly deleted
        if (days > 0) {
            val current = _deletedItems.value.map { it.copy(deletedAt = System.currentTimeMillis()) }
            _deletedItems.value = current
            saveRecycleBin()
        }
        
        cleanOldDeletedItems(days)
    }

    fun cleanOldDeletedItems(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        val current = _deletedItems.value.toMutableList()
        val (toKeep, toRemove) = current.partition { it.deletedAt >= cutoff }
        if (toRemove.isNotEmpty()) {
            _deletedItems.value = toKeep
            saveRecycleBin()
        }
    }
    
    fun getFilteredDeletedItems(): List<DeletedItem> {
        val days = _autoDeleteDays.value
        if (days <= 0) return _deletedItems.value
        val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        return _deletedItems.value.filter { it.deletedAt >= cutoff }
    }

    fun importItems(
        context: Context,
        uri: Uri,
        mergeSingle: Boolean = false,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    ExportImportHelper.importItems(context, uri)
                }
                when (result) {
                    is ExportImportHelper.ImportResult.Success -> {
                        val rawItems = result.items
                        val finalItems: List<Clip> = if (mergeSingle && rawItems.size > 1) {
                            listOf(mergeItemsToSingle(rawItems, context))
                        } else {
                            rawItems
                        }
                        finalItems.forEach { item ->
                            addOrUpdateItem(
                                title = item.title,
                                content = item.content,
                                category = item.category,
                                isTodo = item.isTodo,
                                todoItems = item.todoItems,
                                showSnackbar = false
                            )
                        }
                        withContext(Dispatchers.Main) {
                            onResult(context.getString(R.string.import_success, finalItems.size))
                        }
                    }
                    is ExportImportHelper.ImportResult.Error -> {
                        withContext(Dispatchers.Main) {
                            onResult(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(context.getString(R.string.msg_import_failed, e.message ?: ""))
                }
            } finally {
                _isImporting.value = false
            }
        }
    }

    private fun mergeItemsToSingle(items: List<Clip>, context: Context): Clip {
        val sb = StringBuilder()
        items.forEachIndexed { idx, item ->
            if (idx > 0) sb.append("\n\n---\n\n")
            if (item.title.isNotBlank()) {
                sb.append("## ").append(item.title).append("\n\n")
            }
            if (item.isTodo && item.todoItems.isNotEmpty()) {
                item.todoItems.forEach { todo ->
                    sb.append("- [").append(if (todo.isCompleted) "x" else " ").append("] ")
                    sb.append(todo.title).append("\n")
                }
            } else if (item.content.isNotBlank()) {
                sb.append(item.content)
            }
        }
        val firstTitle = items.firstOrNull { it.title.isNotBlank() }?.title?.take(60)
        val title = firstTitle ?: context.getString(R.string.import_merged_default_title)
        return Clip(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            content = sb.toString().trim(),
            timestamp = System.currentTimeMillis()
        )
    }
}