package com.umkt.ucopy.ui.home

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.common.MultiSelectState
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.fab.FloatingActionMenu
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.state.UiState
import com.umkt.ucopy.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScaffoldLayout(
    drawerState: DrawerState,
    scopeDrawer: CoroutineScope,
    viewModel: ClipViewModel,
    categories: List<String>,
    viewDensity: ViewDensity,
    state: UiState,
    isClipboardListenerRunning: Boolean,
    deletedItemsCount: Int,
    onThemeClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onAboutClick: () -> Unit,
    onListenerClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
    onImportClick: () -> Unit,
    onDesignClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
    onCreateLabelClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    multiSelect: MultiSelectState,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchToggle: () -> Unit,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onSortClick: () -> Unit,
    onViewToggle: () -> Unit,
    onViewLongClick: () -> Unit,
    filteredCount: Int,
    themeMode: ThemeMode,
    onCategoryClick: (String?) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (Clip) -> Unit,
    onEdit: (Clip) -> Unit,
    onDelete: (String) -> Unit,
    onCardClick: (Clip) -> Unit,
    onPinToggle: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedItem: Clip?,
    onBlankNote: () -> Unit,
    onQuickNote: () -> Unit,
    onTodoList: () -> Unit
) {
    HomeDrawer(
        drawerState = drawerState,
        scopeDrawer = scopeDrawer,
        viewModel = viewModel,
        categories = categories,
        viewDensity = viewDensity,
        state = state,
        isClipboardListenerRunning = isClipboardListenerRunning,
        deletedItemsCount = deletedItemsCount,
        onThemeClick = onThemeClick,
        onRecycleBinClick = onRecycleBinClick,
        onAboutClick = onAboutClick,
        onListenerClick = onListenerClick,
        onBackupRestoreClick = onBackupRestoreClick,
        onImportClick = onImportClick,
        onDesignClick = onDesignClick,
        onLanguageClick = onLanguageClick,
        onEditLabelsClick = onEditLabelsClick,
        onCreateLabelClick = onCreateLabelClick,
        onTodoClick = { }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    HomeTopBar(
                        onMenuClick = { scopeDrawer.launch { drawerState.open() } },
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSearchClear = onSearchClear,
                        onSearchToggle = onSearchToggle,
                        multiSelect = multiSelect,
                        state = state,
                        allSelected = allSelected,
                        onSelectAll = onSelectAll,
                        onShareSelected = onShareSelected,
                        onDeleteSelected = onDeleteSelected,
                        onExportClick = onExportClick,
                        onCancelSelection = onCancelSelection,
                        onSortClick = onSortClick,
                        onViewToggle = onViewToggle,
                        onViewLongClick = onViewLongClick,
                        sortOption = state.sortOption,
                        itemCount = filteredCount,
                        currentViewDensity = viewDensity
                    )
                }
            ) { paddingValues ->
                HomeContent(
                    paddingValues = paddingValues,
                    state = state,
                    viewDensity = viewDensity,
                    multiSelect = multiSelect,
                    themeMode = themeMode,
                    onCategoryClick = onCategoryClick,
                    onCopy = onCopy,
                    onShare = onShare,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onCardClick = onCardClick,
                    onPinToggle = onPinToggle,
                    filteredCount = filteredCount,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
            }

            if (!multiSelect.isActive && selectedItem == null) {
                FloatingActionMenu(
                    onBlankNote = onBlankNote,
                    onQuickNote = onQuickNote,
                    onTodoList = onTodoList
                )
            }
        }
    }
}