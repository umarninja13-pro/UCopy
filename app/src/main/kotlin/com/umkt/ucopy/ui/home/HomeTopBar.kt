package com.umkt.ucopy.ui.home

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.LocalDesignConfig
import com.umkt.ucopy.ui.common.MultiSelectState
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.state.UiState
import com.umkt.ucopy.ui.state.SortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeTopBar(
    onMenuClick: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchToggle: () -> Unit,
    multiSelect: MultiSelectState,
    state: UiState,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onSortClick: () -> Unit,
    onViewToggle: () -> Unit,
    onViewLongClick: () -> Unit,
    sortOption: SortOption,
    itemCount: Int,
    currentViewDensity: ViewDensity
) {
    val design = LocalDesignConfig.current

    AnimatedContent(
        targetState = multiSelect.isActive,
        transitionSpec = {
            fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        },
        label = "topbar_animation"
    ) { isSelectionMode ->
        if (isSelectionMode) {
            // --- Selection Mode Top Bar ---
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.selected_count, multiSelect.selectedCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelSelection) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_selection), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll, enabled = state.items.isNotEmpty()) {
                        Icon(
                            if (allSelected) Icons.Filled.CheckBox else Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.select_all),
                            tint = if (state.items.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onShareSelected, enabled = multiSelect.selectedIds.isNotEmpty()) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = if (multiSelect.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                    }
                    IconButton(onClick = onExportClick, enabled = multiSelect.selectedIds.isNotEmpty()) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.export), tint = if (multiSelect.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                    }
                    IconButton(onClick = onDeleteSelected, enabled = multiSelect.selectedIds.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = if (multiSelect.selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            )
        } else {
            // --- Normal Mode Top Bar ---
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(onClick = onSearchToggle)
                                        .padding(start = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp) 
                                ) {
                                    if (design.mode == DesignMode.DEFAULT) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .combinedClickable(
                                                    onClick = onViewToggle,
                                                    onLongClick = onViewLongClick
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (currentViewDensity) {
                                                    ViewDensity.COMPACT -> Icons.Default.ViewCompact
                                                    ViewDensity.COMFORTABLE -> Icons.Default.ViewAgenda
                                                    ViewDensity.GRID -> Icons.Default.GridView
                                                    ViewDensity.DETAILED -> Icons.Default.ViewDay
                                                },
                                                contentDescription = stringResource(R.string.toggle_view),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = onSortClick,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SwapVert,
                                            contentDescription = stringResource(R.string.sort),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}
