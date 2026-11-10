package com.umkt.ucopy.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.umkt.ucopy.R
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.common.MultiSelectState
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.service.ClipboardListenerService
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.state.SortOption
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.utils.BackupHelper
import com.umkt.ucopy.utils.ExportImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFileLaunchers(
    val exportJson: ActivityResultLauncher<String>,
    val exportTxt: ActivityResultLauncher<String>,
    val exportHtml: ActivityResultLauncher<String>,
    val exportPdf: ActivityResultLauncher<String>,
    val exportMd: ActivityResultLauncher<String>,
    val exportCsv: ActivityResultLauncher<String>,
    val exportPng: ActivityResultLauncher<String>,
    val importFile: ActivityResultLauncher<Array<String>>,
    val backupExportJson: ActivityResultLauncher<String>,
    val backupExportZip: ActivityResultLauncher<String>,
    val backupImport: ActivityResultLauncher<Array<String>>,
    val autoBackupPicker: ActivityResultLauncher<String>
)

@Composable
fun rememberHomeFileLaunchers(
    context: Context,
    viewModel: ClipViewModel,
    snackbarHostState: SnackbarHostState,
    multiSelect: MultiSelectState,
    prefs: UserPreferences,
    setRestoredFileAsAutoBackup: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    setIsClipboardListenerRunning: (Boolean) -> Unit,
    setShowLargeImportWarning: (Boolean) -> Unit,
    setPendingImportUri: (Uri?) -> Unit,
    isChangingLocation: Boolean,
    setIsChangingLocation: (Boolean) -> Unit,
    getItemsToExport: () -> List<Clip>,
    isClipboardListenerRunning: Boolean,
    smartImportState: androidx.compose.runtime.MutableState<Boolean>
): HomeFileLaunchers {
    val scope = rememberCoroutineScope()

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            val result = ExportImportHelper.exportItems(context, it, items)
            scope.launch {
                snackbarHostState.showSnackbar(result)
                if (multiSelect.isActive) multiSelect.exitSelectionMode()
            }
        }
    }

    val exportTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            val result = ExportImportHelper.exportToTxt(context, it, items)
            scope.launch {
                snackbarHostState.showSnackbar(result)
                if (multiSelect.isActive) multiSelect.exitSelectionMode()
            }
        }
    }

    val exportHtml = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            val result = ExportImportHelper.exportToHtml(context, it, items)
            scope.launch {
                snackbarHostState.showSnackbar(result)
                if (multiSelect.isActive) multiSelect.exitSelectionMode()
            }
        }
    }

    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            scope.launch {
                try {
                    val result = ExportImportHelper.exportToPdf(context, it, items)
                    snackbarHostState.showSnackbar(result)
                    if (multiSelect.isActive) multiSelect.exitSelectionMode()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_pdf_export_failed, e.message ?: ""))
                }
            }
        }
    }

    val exportMd = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            val result = ExportImportHelper.exportToMarkdown(context, it, items)
            scope.launch {
                snackbarHostState.showSnackbar(result)
                if (multiSelect.isActive) multiSelect.exitSelectionMode()
            }
        }
    }

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            val result = ExportImportHelper.exportToCsv(context, it, items)
            scope.launch {
                snackbarHostState.showSnackbar(result)
                if (multiSelect.isActive) multiSelect.exitSelectionMode()
            }
        }
    }

    val exportPng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        uri?.let {
            val items = getItemsToExport()
            scope.launch {
                try {
                    val result = ExportImportHelper.exportToImage(context, it, items)
                    snackbarHostState.showSnackbar(result)
                    if (multiSelect.isActive) multiSelect.exitSelectionMode()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Image export failed: ${e.message}")
                }
            }
        }
    }

    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val isZipFile = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(it)?.use { input ->
                            val header = ByteArray(4)
                            val read = input.read(header)
                            read == 4 &&
                                header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                                header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                        } ?: false
                    } catch (_: Exception) { false }
                }
                if (isZipFile) {
                    viewModel.importItems(context, it, mergeSingle = !smartImportState.value) { resultMessage ->
                        scope.launch { snackbarHostState.showSnackbar(resultMessage) }
                    }
                    return@launch
                }

                val content = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            inputStream.reader().readText()
                        } ?: ""
                    } catch (_: Exception) { "" }
                }
                val wordCount = content.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                if (wordCount > 15000) {
                    setPendingImportUri(it)
                    setShowLargeImportWarning(true)
                } else {
                    viewModel.importItems(context, it, mergeSingle = !smartImportState.value) { resultMessage ->
                        scope.launch {
                            snackbarHostState.showSnackbar(resultMessage)
                        }
                    }
                }
            }
        }
    }

    val backupExportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val allClips = viewModel.getAllItems()
            val result = BackupHelper.exportBackupAsJson(context, it, allClips, prefs)
            scope.launch { snackbarHostState.showSnackbar(result) }
        }
    }

    val backupExportZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            val allClips = viewModel.getAllItems()
            val result = BackupHelper.exportBackupAsZip(context, it, allClips, prefs)
            scope.launch { snackbarHostState.showSnackbar(result) }
        }
    }

    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val result = BackupHelper.importBackup(context, it)
            when (result) {
                is BackupHelper.ImportResult.Success -> {
                    val settings = result.data.settings
                    val autoBackupKeys = setOf("auto_backup_enabled", "auto_backup_file_uri", "auto_backup_file_name")
                    val filteredSettings = settings.filterKeys { it !in autoBackupKeys }

                    filteredSettings.forEach { (key, value) ->
                        when {
                            key == "clipboard_listener" -> prefs.putBoolean(key, value.toBoolean())
                            key == "time_based_enabled" -> prefs.putBoolean(key, value.toBoolean())
                            key == "app_theme_mode" -> prefs.putString(key, value)
                            key == "view_density" -> prefs.putString(key, value)
                            key == "sort_option" -> prefs.putString(key, value)
                            key.startsWith("time_based_") && key.endsWith("_theme") -> prefs.putString(key, value)
                            key.startsWith("time_based_") && (key.endsWith("_hour") || key.endsWith("_minute")) ->
                                prefs.putInt(key, value.toIntOrNull() ?: 0)
                            else -> prefs.putString(key, value)
                        }
                    }

                    if (setRestoredFileAsAutoBackup) {
                        val fileName = uri.lastPathSegment ?: "backup.json"
                        viewModel.setAutoBackupFile(uri.toString(), fileName)
                        viewModel.setAutoBackupEnabled(true, uri.toString(), fileName)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_auto_backup_set_to_restored)) }
                    }

                    settings["app_theme_mode"]?.let { themeName ->
                        try {
                            val mode = ThemeMode.valueOf(themeName)
                            onThemeChange(mode)
                        } catch (_: Exception) { }
                    }
                    settings["view_density"]?.let { densityName ->
                        try {
                            val density = ViewDensity.valueOf(densityName)
                            viewModel.setViewDensity(density)
                        } catch (_: Exception) { }
                    }
                    settings["clipboard_listener"]?.let { running ->
                        val shouldRun = running.toBoolean()
                        if (shouldRun && !isClipboardListenerRunning) {
                            ClipboardListenerService.start(context)
                            setIsClipboardListenerRunning(true)
                            prefs.putBoolean("clipboard_listener", true)
                        } else if (!shouldRun && isClipboardListenerRunning) {
                            ClipboardListenerService.stop(context)
                            setIsClipboardListenerRunning(false)
                            prefs.putBoolean("clipboard_listener", false)
                        }
                    }
                    settings["sort_option"]?.let { sortName ->
                        try {
                            val sort = SortOption.valueOf(sortName)
                            viewModel.setSortOption(sort)
                        } catch (_: Exception) { }
                    }

                    scope.launch {
                        val previousItems = try { viewModel.getAllItems() } catch (_: Exception) { emptyList() }
                        try {
                            viewModel.clearAllPermanently()
                            viewModel.addAllItems(result.data.clips)
                            viewModel.loadItems()
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_restore_success, result.data.clips.size))
                        } catch (e: Exception) {
                            try {
                                viewModel.clearAllPermanently()
                                viewModel.addAllItems(previousItems)
                                viewModel.loadItems()
                            } catch (_: Exception) { }
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_restore_failed, e.message ?: ""))
                        }
                    }
                }
                is BackupHelper.ImportResult.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(result.message) }
                }
            }
        }
    }

    val autoBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "auto_backup.json"
            viewModel.setAutoBackupFile(uri.toString(), fileName)
            viewModel.setAutoBackupEnabled(true, uri.toString(), fileName)
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_auto_backup_enabled_filename, fileName)) }
        } else {
            if (isChangingLocation) {
                setIsChangingLocation(false)
            } else {
                viewModel.setAutoBackupEnabled(false)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_auto_backup_cancelled)) }
            }
        }
        setIsChangingLocation(false)
    }

    return HomeFileLaunchers(
        exportJson = exportJson,
        exportTxt = exportTxt,
        exportHtml = exportHtml,
        exportPdf = exportPdf,
        exportMd = exportMd,
        exportCsv = exportCsv,
        exportPng = exportPng,
        importFile = importFile,
        backupExportJson = backupExportJson,
        backupExportZip = backupExportZip,
        backupImport = backupImport,
        autoBackupPicker = autoBackupPicker
    )
}