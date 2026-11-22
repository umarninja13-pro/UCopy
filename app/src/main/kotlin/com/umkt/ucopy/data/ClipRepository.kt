package com.umkt.ucopy.data

import android.content.Context
import com.umkt.ucopy.model.Clip
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ClipRepository(private val context: Context) {

    companion object {
        @Volatile
        var dataChangedFromTrash: Boolean = false
    }

    private val dao = AppDatabase.getDatabase(context).clipDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val migrationDone = CompletableDeferred<Unit>()

    init {
        // Automatically migrate old JSON data to Room on first run
        CoroutineScope(Dispatchers.IO).launch {
            try { migrateFromJsonIfNeeded() } finally { migrationDone.complete(Unit) }
        }
    }

    private suspend fun migrateFromJsonIfNeeded() {
        val file = File(context.filesDir, "clips.json")
        if (file.exists()) {
            try {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val oldItems = json.decodeFromString<List<Clip>>(content)
                    if (oldItems.isNotEmpty()) {
                        dao.insertAll(oldItems)
                    }
                }
                file.delete() // Delete JSON after successful migration
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getAll(): List<Clip> = withContext(Dispatchers.IO) {
        migrationDone.await()
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.deleteOldTrash(cutoff)
        val items = dao.getAll()
        val alreadyInitialized = hasBeenInitialized()
        if (items.isEmpty() && !alreadyInitialized) {
            val defaults = createDefaultItems()
            dao.insertAll(defaults)
            markInitialized()
            dao.getAll()
        } else {
            if (!alreadyInitialized) markInitialized()
            items
        }
    }

    private fun hasBeenInitialized(): Boolean =
        File(context.filesDir, "db_initialized.flag").exists()

    private fun markInitialized() {
        try { File(context.filesDir, "db_initialized.flag").createNewFile() } catch (_: Exception) { }
    }

    suspend fun getById(id: String): Clip? = withContext(Dispatchers.IO) {
        migrationDone.await()
        dao.getById(id)
    }

    suspend fun add(item: Clip) = withContext(Dispatchers.IO) {
        dao.insert(item)
    }

    suspend fun addAll(items: List<Clip>) = withContext(Dispatchers.IO) {
        dao.insertAll(items)
    }

    suspend fun update(updatedItem: Clip) = withContext(Dispatchers.IO) {
        val current = dao.getById(updatedItem.id)
        if (current != null) {
            dao.update(updatedItem.copy(timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        softDelete(id)
    }

    suspend fun deleteAll(ids: List<String>) = withContext(Dispatchers.IO) {
        softDeleteAll(ids)
    }

    suspend fun deletePermanently(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deletePermanentlyAll(ids: List<String>) = withContext(Dispatchers.IO) {
        dao.deleteByIds(ids)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun softDelete(id: String) = withContext(Dispatchers.IO) {
        val current = dao.getById(id)
        if (current != null) {
            dao.update(current.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
        }
    }

    suspend fun softDeleteAll(ids: List<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val current = dao.getById(id)
            if (current != null) {
                dao.update(current.copy(isDeleted = true, deletedAt = now))
            }
        }
    }

    suspend fun getTrash(): List<Clip> = withContext(Dispatchers.IO) {
        migrationDone.await()
        dao.getTrash()
    }

    fun getTrashFlow(): Flow<List<Clip>> = dao.getTrashFlow()

    suspend fun purgeOldTrash() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.deleteOldTrash(cutoff)
    }

    suspend fun restoreFromTrash(id: String) = withContext(Dispatchers.IO) {
        dao.restoreFromTrash(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        dao.emptyTrash()
    }

    suspend fun getAllCategories(): List<String> = withContext(Dispatchers.IO) {
        dao.getCategoryStrings().flatMap { it.split(", ") }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun createDefaultItems(): List<Clip> {
        val now = System.currentTimeMillis()
        return listOf(
            Clip(
                id = UUID.randomUUID().toString(),
                title = "👋 Welcome to U Copy",
                content = """
                    Welcome to U Copy!
                    
                    Your ultimate offline clipboard manager. Fast, simple, and completely private.
                    
                    🔒 100% Offline & Private:
                    Your data never leaves your device. We don't use any network or internet.
                    
                    ☁️ Auto-Backup & Restore:
                    Set up auto-backup once, and every change you make is saved automatically! You can also manually export your data anytime.
                    
                    ✨ More Features:
                    - Pin important notes to the top.
                    - Create interactive checklists (To-dos).
                    - Beautiful themes and clean design.
                    
                    Tap the + button to create your first note!
                """.trimIndent(),
                timestamp = now + 1,
                isPinned = true,
                category = null
            ),
            Clip(
                id = UUID.randomUUID().toString(),
                title = "✨ Welcome to Rich Text!",
                content = """
                    # Markdown Magic
                    
                    U Copy supports Markdown to make your notes beautiful and organized.
                    
                    ### Text Styles:
                    You can make text **bold**, *italic*, or ~~strikethrough~~.
                    
                    ### Lists:
                    *   Shopping list
                    *   Project ideas
                    *   Meeting notes
                    
                    ### Quotes:
                    > "Simplicity is the ultimate sophistication."
                    
                    ### Code Blocks:
                    ```kotlin
                    fun greet(name: String) {
                        println("Hello, ${'$'}name!")
                    }
                    ```
                    
                    **Pro Tip:** Tap this note to edit and see how the formatting works!
                """.trimIndent(),
                timestamp = now,
                isPinned = false,
                category = "Features"
            )
        )
    }
}
