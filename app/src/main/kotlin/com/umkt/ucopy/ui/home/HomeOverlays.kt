package com.umkt.ucopy.ui.home

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.about.AboutDialog
import com.umkt.ucopy.ui.backup.BackupRestoreDialog
import com.umkt.ucopy.ui.design.ActionPreferences
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.importnotes.ImportDialog
import com.umkt.ucopy.ui.editor.AddEditDialog
import com.umkt.ucopy.ui.label.LabelEditScreen
import com.umkt.ucopy.ui.search.SearchScreen
import com.umkt.ucopy.ui.sheet.ExportBottomSheet
import com.umkt.ucopy.ui.sheet.NewClipDialog
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.state.SortOption
import com.umkt.ucopy.ui.state.UiState
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.ui.trash.RecycleBinScreen
import com.umkt.ucopy.ui.ThemeDialog
import com.umkt.ucopy.ui.design.DesignSelectorDialog
import com.umkt.ucopy.ui.service.ListenerSetupDialog
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.state.DeletedItem
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeOverlays(
    launchers: HomeFileLaunchers,
    viewModel: ClipViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    isImporting: Boolean,

    showSearchScreen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismissSearch: () -> Unit,
    state: UiState,
    onResultClick: (Clip) -> Unit,
    categories: List<String>,
    onCategoryClick: (String?) -> Unit,

    showThemeDialog: Boolean,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDismissTheme: () -> Unit,
    onThemeConfigSaved: () -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,

    showAddEdit: Boolean,
    editingItem: Clip?,
    onDismissAddEdit: () -> Unit,
    onSaveAddEdit: (String, String, String?) -> Unit,

    showAbout: Boolean,
    onDismissAbout: () -> Unit,

    showRecycleBin: Boolean,
    deletedItems: List<DeletedItem>,
    autoDeleteDays: Int,
    onRestoreItem: (String) -> Unit,
    onRestoreAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismissRecycleBin: () -> Unit,
    onAutoDeleteDaysChange: (Int) -> Unit,

    showBackupDialog: Boolean,
    autoBackupEnabled: Boolean,
    autoBackupFileUri: String?,
    autoBackupFileName: String,
    setRestoredFileAsAutoBackup: Boolean,
    onAutoBackupToggle: (Boolean) -> Unit,
    onChangeLocation: () -> Unit,
    onRestoreBackup: () -> Unit,
    onExportJson: () -> Unit,
    onExportZip: () -> Unit,
    onSetAsAutoBackupChanged: (Boolean) -> Unit,
    onDismissBackup: () -> Unit,

    showLanguageDialog: Boolean,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismissLanguage: () -> Unit,

    showImportDialog: Boolean,
    onChooseImportFile: () -> Unit,
    onDismissImport: () -> Unit,
    isSmartMode: Boolean,
    onSmartModeChange: (Boolean) -> Unit,

    showDesignSelector: Boolean,
    designMode: DesignMode,
    actionPrefs: ActionPreferences,
    onSelectDesign: (DesignMode) -> Unit,
    onDismissDesign: () -> Unit,
    onActionPrefsChange: (ActionPreferences) -> Unit,

    showLabelEditScreen: Boolean,
    startLabelInCreateMode: Boolean,
    onRenameLabel: (String, String) -> Unit,
    onDeleteLabel: (String) -> Unit,
    onCreateLabel: (String) -> Unit,
    onDismissLabel: () -> Unit,

    showListenerSetupDialog: Boolean,
    onCompleteListenerSetup: () -> Unit,
    onDismissListenerSetup: () -> Unit,

    showNewClipDialog: Boolean,
    newClipText: String?,
    onSaveNewClip: (String) -> Unit,
    onDismissNewClip: () -> Unit,

    showExportSheet: Boolean,
    onExportFormat: (String) -> Unit,
    onDismissExportSheet: () -> Unit,

    showSortSheet: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onDismissSortSheet: () -> Unit,

    showViewSheet: Boolean,
    viewDensity: ViewDensity,
    onDensitySelected: (ViewDensity) -> Unit,
    onDismissViewSheet: () -> Unit,

    showLargeImportWarning: Boolean,
    pendingImportUri: Uri?,
    onConfirmLargeImport: () -> Unit,
    onDismissLargeImport: () -> Unit
) {
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

    if (showSearchScreen) {
        SearchScreen(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onDismiss = onDismissSearch,
            results = state.items,
            onResultClick = onResultClick,
            categories = categories,
            activeCategory = state.selectedCategory,
            onCategoryClick = onCategoryClick
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            onThemeChange = onThemeChange,
            onDismiss = onDismissTheme,
            onThemeConfigSaved = onThemeConfigSaved,
            onDynamicColorToggle = onDynamicColorToggle
        )
    }

    if (showAddEdit) {
        AddEditDialog(
            item = editingItem,
            categories = categories,
            onDismiss = onDismissAddEdit,
            onSave = onSaveAddEdit,
            viewModel = viewModel
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = onDismissAbout)
    }

    if (showRecycleBin) {
        RecycleBinScreen(
            deletedItems = deletedItems,
            onRestore = onRestoreItem,
            onRestoreAll = onRestoreAll,
            onClearAll = onClearAll,
            onDismiss = onDismissRecycleBin,
            autoDeleteDays = autoDeleteDays,
            onAutoDeleteDaysChange = onAutoDeleteDaysChange
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            onDismiss = onDismissBackup,
            autoBackupEnabled = autoBackupEnabled,
            onAutoBackupToggle = onAutoBackupToggle,
            hasFile = autoBackupFileUri != null,
            fileName = autoBackupFileName,
            onChangeLocation = onChangeLocation,
            onRestoreBackup = onRestoreBackup,
            onExportJson = onExportJson,
            onExportZip = onExportZip,
            setAsAutoBackup = setRestoredFileAsAutoBackup,
            onSetAsAutoBackupChanged = onSetAsAutoBackupChanged
        )
    }

    if (showDesignSelector) {
        DesignSelectorDialog(
            currentMode = designMode,
            onSelect = onSelectDesign,
            onDismiss = onDismissDesign,
            actionPrefs = actionPrefs,
            onActionPrefsChange = onActionPrefsChange
        )
    }

    if (showLabelEditScreen) {
        LabelEditScreen(
            labels = categories,
            onRename = onRenameLabel,
            onDelete = onDeleteLabel,
            onCreate = onCreateLabel,
            onDismiss = onDismissLabel,
            startInCreateMode = startLabelInCreateMode
        )
    }

    if (showListenerSetupDialog) {
        ListenerSetupDialog(
            onDismiss = onDismissListenerSetup,
            onComplete = onCompleteListenerSetup
        )
    }

    if (showNewClipDialog && newClipText != null) {
        NewClipDialog(
            clipText = newClipText,
            onDismiss = onDismissNewClip,
            onSave = onSaveNewClip
        )
    }

    if (showExportSheet) {
        ExportBottomSheet(
            onDismiss = onDismissExportSheet,
            onExport = onExportFormat
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = state.sortOption,
            onSortSelected = onSortSelected,
            onDismiss = onDismissSortSheet
        )
    }

    if (showViewSheet) {
        ViewDensityBottomSheet(
            currentDensity = viewDensity,
            onDensitySelected = onDensitySelected,
            onDismiss = onDismissViewSheet
        )
    }

    if (showLanguageDialog) {
        com.umkt.ucopy.ui.dialogs.LanguageDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            onDismiss = onDismissLanguage
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = onDismissImport,
            onChooseFile = onChooseImportFile,
            isSmartMode = isSmartMode,
            onSmartModeChange = onSmartModeChange
        )
    }

    if (showLargeImportWarning && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = onDismissLargeImport,
            title = { Text(stringResource(R.string.large_file_warning)) },
            text = {
                Text(stringResource(R.string.large_file_message))
            },
            confirmButton = {
                TextButton(onClick = onConfirmLargeImport) {
                    Text(stringResource(R.string.continue_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLargeImport) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}