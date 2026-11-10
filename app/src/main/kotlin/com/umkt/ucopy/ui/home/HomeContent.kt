package com.umkt.ucopy.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.R
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.LocalDesignConfig
import com.umkt.ucopy.ui.design.LocalActionPreferences
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.ui.card.ClipCard
import com.umkt.ucopy.ui.card.NotebookCard
import com.umkt.ucopy.ui.card.ListCard
import com.umkt.ucopy.ui.card.TaskCard
import com.umkt.ucopy.ui.common.MultiSelectState
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.state.UiState
import com.umkt.ucopy.ui.state.SortOption

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    state: UiState,
    viewDensity: ViewDensity,
    multiSelect: MultiSelectState,
    themeMode: ThemeMode,
    onCategoryClick: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (Clip) -> Unit,
    onEdit: (Clip) -> Unit,
    onDelete: (String) -> Unit,
    onCardClick: (Clip) -> Unit,
    onPinToggle: (String) -> Unit,
    filteredCount: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {

    val design = LocalDesignConfig.current
    val actionPrefs = LocalActionPreferences.current

    if (state.items.isEmpty() && !state.hasCompletedInitialLoad) {
        // RULE 2 & 7: Blank screen on cold start, no spinner, no "No notes" flash
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(paddingValues))
        return
    }

    if (state.items.isEmpty()) {
        val isSearching = state.searchQuery.isNotBlank()
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isSearching -> "\uD83D\uDD0D"
                    state.selectedCategory != null -> "\uD83C\uDFAF"
                    else -> "\uD83D\uDCDD"
                },
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    isSearching -> stringResource(R.string.no_results_for, state.searchQuery)
                    state.selectedCategory != null -> stringResource(R.string.no_items_filter)
                    else -> stringResource(R.string.no_clips)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isSearching -> stringResource(R.string.try_search)
                    state.selectedCategory != null -> stringResource(R.string.adjust_filters)
                    else -> stringResource(R.string.tap_add)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    @Composable
    fun FilterChips() {
        val category = state.selectedCategory
        if (category != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { onCategoryClick("") },
                    label = { Text(category) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_filter), modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues).pullRefresh(pullRefreshState)) {
        val showAnim = actionPrefs.showListAnimations && state.hasCompletedInitialLoad
        val (pinnedItems, otherItems) = remember(state.items) {
            state.items.partition { it.isPinned }
        }
        
        AnimatedContent(
            targetState = viewDensity,
            transitionSpec = {
                if (showAnim) {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                } else {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                }
            },
            label = "view_transition"
        ) { currentDensity ->
            if (currentDensity == ViewDensity.GRID && design.mode == DesignMode.DEFAULT) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FilterChips()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pinnedItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = stringResource(R.string.pinned),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            gridItems(pinnedItems, key = { it.id }) { item ->
                                ClipCard(
                                    item = item,
                                    modifier = if (actionPrefs.showListAnimations) Modifier.fillMaxWidth().animateItem() else Modifier.fillMaxWidth(),
                                    themeMode = themeMode,
                                    viewDensity = currentDensity,
                                    highlightQuery = state.searchQuery,
                                    isSelectionMode = multiSelect.isActive,
                                    isSelected = multiSelect.isSelected(item.id),
                                    onSelectionToggle = { multiSelect.toggleSelection(item.id) },
                                    onLongPress = { multiSelect.enterSelectionMode(item.id) },
                                    onCopy = { onCopy(item.content) },
                                    onShare = { onShare(item) },
                                    onEdit = { onEdit(item) },
                                    onDelete = { onDelete(item.id) },
                                    onCardClick = { onCardClick(item) },
                                    onPinToggle = { },
                                    onFavoriteToggle = { },
                                    onCategoryClick = onCategoryClick
                                )
                            }
                        }

                        if (otherItems.isNotEmpty()) {
                            if (pinnedItems.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = stringResource(R.string.others),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(start = 16.dp, top = if (pinnedItems.isEmpty()) 16.dp else 24.dp, bottom = 8.dp)
                                    )
                                }
                            }
                            gridItems(otherItems, key = { it.id }) { item ->
                                ClipCard(
                                    item = item,
                                    modifier = if (actionPrefs.showListAnimations) Modifier.fillMaxWidth().animateItem() else Modifier.fillMaxWidth(),
                                    themeMode = themeMode,
                                    viewDensity = currentDensity,
                                    highlightQuery = state.searchQuery,
                                    isSelectionMode = multiSelect.isActive,
                                    isSelected = multiSelect.isSelected(item.id),
                                    onSelectionToggle = { multiSelect.toggleSelection(item.id) },
                                    onLongPress = { multiSelect.enterSelectionMode(item.id) },
                                    onCopy = { onCopy(item.content) },
                                    onShare = { onShare(item) },
                                    onEdit = { onEdit(item) },
                                    onDelete = { onDelete(item.id) },
                                    onCardClick = { onCardClick(item) },
                                    onPinToggle = { },
                                    onFavoriteToggle = { },
                                    onCategoryClick = onCategoryClick
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.selectedCategory != null) {
                        item { FilterChips() }
                    }

                    if (pinnedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.pinned),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(pinnedItems, key = { it.id }) { item ->
                            val isSelected = multiSelect.isSelected(item.id)
                            val onSelToggle = { multiSelect.toggleSelection(item.id) }
                            val onLong = { multiSelect.enterSelectionMode(item.id) }
                            val onCardTap = { if (multiSelect.isActive) onSelToggle() else onCardClick(item) }

                            when (design.mode) {
                                DesignMode.DEFAULT -> {
                                    ClipCard(
                                        item = item,
                                        themeMode = themeMode,
                                        viewDensity = currentDensity,
                                        highlightQuery = state.searchQuery,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onSelectionToggle = onSelToggle,
                                        onLongPress = onLong,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onCardClick = { onCardTap() },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        modifier = if (actionPrefs.showListAnimations) Modifier.fillMaxWidth().animateItem() else Modifier.fillMaxWidth()
                                    )
                                }
                                DesignMode.NOTEBOOKS -> {
                                    NotebookCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                                DesignMode.LIST -> {
                                    ListCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                                DesignMode.TASK -> {
                                    TaskCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                            }
                        }
                    }

                    if (otherItems.isNotEmpty()) {
                        if (pinnedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.others),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 20.dp, top = if (pinnedItems.isEmpty()) 16.dp else 24.dp, bottom = 8.dp)
                                )
                            }
                        }
                        items(otherItems, key = { it.id }) { item ->
                            val isSelected = multiSelect.isSelected(item.id)
                            val onSelToggle = { multiSelect.toggleSelection(item.id) }
                            val onLong = { multiSelect.enterSelectionMode(item.id) }
                            val onCardTap = { if (multiSelect.isActive) onSelToggle() else onCardClick(item) }

                            when (design.mode) {
                                DesignMode.DEFAULT -> {
                                    ClipCard(
                                        item = item,
                                        themeMode = themeMode,
                                        viewDensity = currentDensity,
                                        highlightQuery = state.searchQuery,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onSelectionToggle = onSelToggle,
                                        onLongPress = onLong,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onCardClick = { onCardTap() },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        modifier = if (actionPrefs.showListAnimations) Modifier.fillMaxWidth().animateItem() else Modifier.fillMaxWidth()
                                    )
                                }
                                DesignMode.NOTEBOOKS -> {
                                    NotebookCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                                DesignMode.LIST -> {
                                    ListCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                                DesignMode.TASK -> {
                                    TaskCard(
                                        item = item,
                                        isSelectionMode = multiSelect.isActive,
                                        isSelected = isSelected,
                                        onCopy = { onCopy(item.content) },
                                        onShare = { onShare(item) },
                                        onEdit = { onEdit(item) },
                                        onDelete = { onDelete(item.id) },
                                        onPinToggle = { },
                                        onFavoriteToggle = { },
                                        onCategoryClick = onCategoryClick,
                                        onClick = { onCardTap() },
                                        onLongClick = { onLong() }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
