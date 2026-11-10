package com.umkt.ucopy.ui.home

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.utils.BackupHelper
import com.umkt.ucopy.utils.ExportImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.umkt.ucopy.ui.detail.DetailScreen
import com.umkt.ucopy.ui.about.AboutDialog
import com.umkt.ucopy.ui.common.MultiSelectState
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.service.ClipboardListenerService
import com.umkt.ucopy.ui.service.ListenerSetupDialog
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.fab.FloatingActionMenu
import com.umkt.ucopy.ui.editor.AddEditDialog
import com.umkt.ucopy.ui.label.LabelEditScreen
import com.umkt.ucopy.ui.search.SearchScreen
import com.umkt.ucopy.ui.trash.RecycleBinScreen
import com.umkt.ucopy.ui.sheet.ExportBottomSheet
import com.umkt.ucopy.ui.sheet.NewClipDialog
import com.umkt.ucopy.ui.backup.BackupRestoreDialog
import com.umkt.ucopy.ui.state.SortOption
import com.umkt.ucopy.ui.ThemeDialog
import com.umkt.ucopy.ui.shareItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ClipViewModel,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onThemeConfigSaved: () -> Unit = {},
    onDynamicColorToggle: (Boolean) -> Unit,
    designMode: com.umkt.ucopy.ui.design.DesignMode,
    onDesignChange: (com.umkt.ucopy.ui.design.DesignMode) -> Unit,
    actionPrefs: com.umkt.ucopy.ui.design.ActionPreferences,
    onActionPrefsChange: (com.umkt.ucopy.ui.design.ActionPreferences) -> Unit,
    clipToOpen: Clip? = null,
    onClipOpened: () -> Unit = {},
    sharedItemToOpen: Clip? = null,
    onSharedItemOpened: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val viewDensity by viewModel.viewDensity.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val deletedItems by viewModel.deletedItems.collectAsState()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()
    val filteredDeletedItems = remember(deletedItems, autoDeleteDays) { viewModel.getFilteredDeletedItems() }
    val isImporting by viewModel.isImporting.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val json = Json { ignoreUnknownKeys = true }

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val autoBackupFileUri by viewModel.autoBackupFileUri.collectAsState()
    val autoBackupFileName by viewModel.autoBackupFileName.collectAsState()

    var showAddEdit by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Clip?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Clip?>(clipToOpen) }
    var showRecycleBin by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDesignSelector by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLabelEditScreen by remember { mutableStateOf(false) }
    var startLabelInCreateMode by remember { mutableStateOf(false) }
    
    var showSortSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showListenerSetupDialog by remember { mutableStateOf(false) }
    var newClipText by remember { mutableStateOf<String?>(null) }
    var showNewClipDialog by remember { mutableStateOf(false) }
    var showViewSheet by remember { mutableStateOf(false) }

    var setRestoredFileAsAutoBackup by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val multiSelect = remember { MultiSelectState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeDrawer = rememberCoroutineScope()

    val prefs = UserPreferences(context)
    val smartImportState = remember { mutableStateOf(prefs.getBoolean("smart_import_enabled", false)) }

    var isClipboardListenerRunning by remember {
        mutableStateOf(prefs.getBoolean("clipboard_listener", false))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME ||
                event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                val running = prefs.getBoolean("clipboard_listener", false)
                if (isClipboardListenerRunning != running) {
                    isClipboardListenerRunning = running
                }
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }



    var isChangingLocation by remember { mutableStateOf(false) }

    var showLargeImportWarning by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(clipToOpen) {
        if (clipToOpen != null) {
            selectedItem = clipToOpen
            onClipOpened()
        }
    }

    LaunchedEffect(sharedItemToOpen) {
        if (sharedItemToOpen != null) {
            selectedItem = sharedItemToOpen
            onSharedItemOpened()
        }
    }

    val onRefresh = {
        isRefreshing = true
        viewModel.loadItems()
    }

    LaunchedEffect(state.isLoading, isRefreshing) {
        if (!state.isLoading && isRefreshing) {
            isRefreshing = false
        }
    }

    rememberClipboardReceiver(
        context = context,
        viewModel = viewModel,
        prefs = prefs,
        onListenerStateChange = { isClipboardListenerRunning = it },
        onNewClipDetected = { text ->
            newClipText = text
            showNewClipDialog = true
        }
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ClipboardListenerService.start(context)
            isClipboardListenerRunning = true
            prefs.putBoolean("clipboard_listener", true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_permission_required_notifications))
            }
        }
    }

    fun getItemsToExport(): List<Clip> {
        return if (multiSelect.isActive && multiSelect.selectedIds.isNotEmpty()) {
            multiSelect.getSelectedItems(state.items)
        } else {
            state.items
        }
    }

    val launchers = rememberHomeFileLaunchers(
        context = context,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        multiSelect = multiSelect,
        prefs = prefs,
        setRestoredFileAsAutoBackup = setRestoredFileAsAutoBackup,
        onThemeChange = onThemeChange,
        setIsClipboardListenerRunning = { isClipboardListenerRunning = it },
        setShowLargeImportWarning = { showLargeImportWarning = it },
        setPendingImportUri = { pendingImportUri = it },
        isChangingLocation = isChangingLocation,
        setIsChangingLocation = { isChangingLocation = it },
        getItemsToExport = { getItemsToExport() },
        isClipboardListenerRunning = isClipboardListenerRunning,
        smartImportState = smartImportState
    )

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    fun deleteSelected() {
        val ids = multiSelect.selectedIds.toList()
        if (ids.isNotEmpty()) {
            viewModel.deleteSelected(ids)
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_items_deleted, ids.size))
            }
            multiSelect.exitSelectionMode()
        }
    }

    fun shareSelected() {
        val selectedItems = multiSelect.getSelectedItems(state.items)
        if (selectedItems.isNotEmpty()) {
            val combinedText = selectedItems.joinToString("\n\n---\n\n") { it.content }
            viewModel.shareText(context, combinedText)
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_shared_items, selectedItems.size))
            }
            multiSelect.exitSelectionMode()
        }
    }

    val allSelected = state.items.isNotEmpty() && state.items.all { multiSelect.isSelected(it.id) }
    val filteredCount = state.items.size

    BackHandler(enabled = multiSelect.isActive) {
        multiSelect.exitSelectionMode()
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.importing)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.importing_wait))
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    HomeScaffoldLayout(
        drawerState = drawerState,
        scopeDrawer = scopeDrawer,
        viewModel = viewModel,
        categories = categories,
        viewDensity = viewDensity,
        state = state,
        isClipboardListenerRunning = isClipboardListenerRunning,
        deletedItemsCount = filteredDeletedItems.size,
        onThemeClick = { showThemeDialog = true },
        onRecycleBinClick = { showRecycleBin = true },
        onAboutClick = { showAbout = true },
        onListenerClick = {
            if (isClipboardListenerRunning) {
                ClipboardListenerService.stop(context)
                isClipboardListenerRunning = false
                prefs.putBoolean("clipboard_listener", false)
            } else {
                if (prefs.getBoolean("listener_setup_completed", false)) {
                    ClipboardListenerService.start(context)
                    isClipboardListenerRunning = true
                    prefs.putBoolean("clipboard_listener", true)
                } else {
                    showListenerSetupDialog = true
                }
            }
        },
        onBackupRestoreClick = { showBackupDialog = true },
        onImportClick = { showImportDialog = true },
        onDesignClick = { showDesignSelector = true },
        onLanguageClick = { showLanguageDialog = true },
        onEditLabelsClick = { startLabelInCreateMode = false; showLabelEditScreen = true },
        onCreateLabelClick = { startLabelInCreateMode = true; showLabelEditScreen = true },
        snackbarHostState = snackbarHostState,
        multiSelect = multiSelect,
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchQueryChange = { query ->
            searchQuery = query
            viewModel.search(query)
        },
        onSearchClear = {
            searchQuery = ""
            viewModel.search("")
        },
        onSearchToggle = {
            searchQuery = ""
            viewModel.search("")
            viewModel.setCategory(null)
            showSearchScreen = true
        },
        allSelected = allSelected,
        onSelectAll = {
            if (allSelected) {
                multiSelect.clearSelection()
            } else {
                multiSelect.selectAll(state.items.map { it.id })
            }
        },
        onShareSelected = { shareSelected() },
        onDeleteSelected = { showDeleteConfirmDialog = true },
        onExportClick = { showExportSheet = true },
        onCancelSelection = { multiSelect.exitSelectionMode() },
        onSortClick = { showSortSheet = true },
        onViewToggle = {
            val nextDensity = when (viewDensity) {
                ViewDensity.COMFORTABLE -> ViewDensity.GRID
                ViewDensity.GRID -> ViewDensity.COMPACT
                ViewDensity.COMPACT -> ViewDensity.DETAILED
                ViewDensity.DETAILED -> ViewDensity.COMFORTABLE
            }
            viewModel.setViewDensity(nextDensity)
        },
        onViewLongClick = { showViewSheet = true },
        filteredCount = filteredCount,
        themeMode = themeMode,
        onCategoryClick = { category ->
            if (category.isNullOrEmpty()) {
                viewModel.setCategory(null)
            } else {
                viewModel.setCategory(category)
            }
        },
        onCopy = { viewModel.copyToClipboard(it) },
        onShare = { shareItem(context, it) },
        onEdit = { item ->
            editingItem = item
            showAddEdit = true
        },
        onDelete = { id ->
            viewModel.deleteItem(id)
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_item_deleted))
            }
        },
        onCardClick = { item -> selectedItem = item },
        onPinToggle = { viewModel.togglePin(it) },
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        selectedItem = selectedItem,
        onBlankNote = {
            val newItem = Clip(
                id = "",
                title = "",
                content = "",
                timestamp = System.currentTimeMillis()
            )
            selectedItem = newItem
        },
        onQuickNote = { showAddEdit = true },
        onTodoList = {
            val newItem = Clip(
                id = "",
                title = "",
                content = "",
                isTodo = true,
                timestamp = System.currentTimeMillis()
            )
            selectedItem = newItem
        }
    )

    HomeDetailOverlay(
        selectedItem = selectedItem,
        clipToOpen = clipToOpen,
        actionPrefs = actionPrefs,
        themeMode = themeMode,
        categories = categories,
        viewModel = viewModel,
        onDismiss = { selectedItem = null },
        snackbarHostState = snackbarHostState,
        scope = scope
    )

    HomeDeleteDialog(
        show = showDeleteConfirmDialog,
        selectedCount = multiSelect.selectedCount,
        onDismiss = { showDeleteConfirmDialog = false },
        onConfirm = {
            showDeleteConfirmDialog = false
            deleteSelected()
        }
    )

    HomeOverlays(
        launchers = launchers,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        scope = scope,
        isImporting = isImporting,
        showSearchScreen = showSearchScreen,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it; viewModel.search(it) },
        onDismissSearch = { showSearchScreen = false; searchQuery = ""; viewModel.search(""); viewModel.setCategory(null) },
        state = state,
        onResultClick = { item -> showSearchScreen = false; selectedItem = item },
        categories = categories,
        onCategoryClick = { cat -> viewModel.setCategory(cat) },
        showThemeDialog = showThemeDialog,
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        onThemeChange = { newTheme -> onThemeChange(newTheme); viewModel.performAutoBackup() },
        onDismissTheme = { showThemeDialog = false },
        onThemeConfigSaved = { onThemeConfigSaved(); viewModel.performAutoBackup() },
        onDynamicColorToggle = { enabled -> onDynamicColorToggle(enabled); viewModel.performAutoBackup() },
        showAddEdit = showAddEdit,
        editingItem = editingItem,
        onDismissAddEdit = { showAddEdit = false; editingItem = null },
        onSaveAddEdit = { title, content, category ->
            viewModel.addOrUpdateItem(title, content, existingId = editingItem?.id, category = category)
            showAddEdit = false
            editingItem = null
        },
        showAbout = showAbout,
        onDismissAbout = { showAbout = false },
        showRecycleBin = showRecycleBin,
        deletedItems = filteredDeletedItems,
        autoDeleteDays = autoDeleteDays,
        onRestoreItem = { viewModel.restoreItem(it) },
        onRestoreAll = { viewModel.restoreAll() },
        onClearAll = { viewModel.clearDeletedItems() },
        onDismissRecycleBin = { showRecycleBin = false },
        onAutoDeleteDaysChange = { viewModel.setAutoDeleteDays(it) },
        showBackupDialog = showBackupDialog,
        autoBackupEnabled = autoBackupEnabled,
        autoBackupFileUri = autoBackupFileUri,
        autoBackupFileName = autoBackupFileName,
        setRestoredFileAsAutoBackup = setRestoredFileAsAutoBackup,
        onAutoBackupToggle = { checked ->
            if (checked && autoBackupFileUri == null) {
                isChangingLocation = false
                launchers.autoBackupPicker.launch("auto_backup.json")
            } else if (checked && autoBackupFileUri != null) {
                viewModel.setAutoBackupEnabled(true)
            } else {
                viewModel.setAutoBackupEnabled(false)
            }
        },
        onChangeLocation = {
            isChangingLocation = true
            launchers.autoBackupPicker.launch("auto_backup.json")
        },
        onRestoreBackup = {
            showBackupDialog = false
            launchers.backupImport.launch(arrayOf("application/json", "application/zip"))
        },
        onExportJson = {
            showBackupDialog = false
            launchers.backupExportJson.launch("ucopy_backup.json")
        },
        onExportZip = {
            showBackupDialog = false
            launchers.backupExportZip.launch("ucopy_backup.zip")
        },
        onSetAsAutoBackupChanged = { setRestoredFileAsAutoBackup = it },
        onDismissBackup = { showBackupDialog = false },
        showLanguageDialog = showLanguageDialog,
        currentLanguage = run {
            val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) "system" else locales.toLanguageTags()
        },
        onLanguageSelected = { code ->
            if (code == "system") {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                )
            } else {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(code)
                )
            }
        },
        onDismissLanguage = { showLanguageDialog = false },
        
        showImportDialog = showImportDialog,
        isSmartMode = smartImportState.value,
        onSmartModeChange = { 
            smartImportState.value = it
            prefs.putBoolean("smart_import_enabled", it)
        },
        onChooseImportFile = {
            showImportDialog = false
            launchers.importFile.launch(arrayOf(
                "application/json",
                "text/plain",
                "text/html",
                "text/markdown",
                "text/x-markdown",
                "text/csv",
                "text/comma-separated-values",
                "application/xml",
                "text/xml",
                "application/zip",
                "application/x-zip-compressed"
            ))
        },
        onDismissImport = { showImportDialog = false },
        showDesignSelector = showDesignSelector,
        designMode = designMode,
        actionPrefs = actionPrefs,
        onSelectDesign = { onDesignChange(it) },
        onDismissDesign = { showDesignSelector = false },
        onActionPrefsChange = { onActionPrefsChange(it) },
        showLabelEditScreen = showLabelEditScreen,
        startLabelInCreateMode = startLabelInCreateMode,
        onRenameLabel = { old, new -> viewModel.renameLabel(old, new) },
        onDeleteLabel = { name -> viewModel.deleteLabel(name) },
        onCreateLabel = { name -> viewModel.addLabel(name) },
        onDismissLabel = { showLabelEditScreen = false },
        showListenerSetupDialog = showListenerSetupDialog,
        onCompleteListenerSetup = {
            showListenerSetupDialog = false
            prefs.putBoolean("listener_setup_completed", true)
            ClipboardListenerService.start(context)
            isClipboardListenerRunning = true
            prefs.putBoolean("clipboard_listener", true)
        },
        onDismissListenerSetup = { showListenerSetupDialog = false },
        showNewClipDialog = showNewClipDialog,
        newClipText = newClipText,
        onSaveNewClip = { text ->
            val title = text.lines().firstOrNull()?.take(60) ?: context.getString(R.string.quick_clip_default_title)
            viewModel.addOrUpdateItem(title, text, category = null)
            showNewClipDialog = false
            newClipText = null
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_clip_saved_success)) }
        },
        onDismissNewClip = { showNewClipDialog = false; newClipText = null },
        showExportSheet = showExportSheet,
        onExportFormat = { format ->
            showExportSheet = false
            when (format) {
                "JSON" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportJson.launch("ucopy_selected.json")
                "TXT" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportTxt.launch("ucopy_selected.txt")
                "HTML" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportHtml.launch("ucopy_selected.html")
                "PDF" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportPdf.launch("ucopy_selected.pdf")
                "MD" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportMd.launch("ucopy_selected.md")
                "CSV" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportCsv.launch("ucopy_selected.csv")
                "PNG" -> if (multiSelect.selectedIds.isNotEmpty()) launchers.exportPng.launch("ucopy_selected.png")
            }
        },
        onDismissExportSheet = { showExportSheet = false },
        showSortSheet = showSortSheet,
        onSortSelected = { option -> viewModel.setSortOption(option); showSortSheet = false },
        onDismissSortSheet = { showSortSheet = false },
        showViewSheet = showViewSheet,
        viewDensity = viewDensity,
        onDensitySelected = { density -> viewModel.setViewDensity(density); showViewSheet = false },
        onDismissViewSheet = { showViewSheet = false },
        showLargeImportWarning = showLargeImportWarning,
        pendingImportUri = pendingImportUri,
        onConfirmLargeImport = {
            val uri = pendingImportUri
            pendingImportUri = null
            showLargeImportWarning = false
            uri?.let {
                viewModel.importItems(context, it, mergeSingle = !smartImportState.value) { resultMessage ->
                    scope.launch {
                        snackbarHostState.showSnackbar(resultMessage)
                        viewModel.acknowledgeAllCurrentNotes()
                    }
                }
            }
        },
        onDismissLargeImport = { showLargeImportWarning = false; pendingImportUri = null }
    )
}
