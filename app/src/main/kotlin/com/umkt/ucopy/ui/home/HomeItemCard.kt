package com.umkt.ucopy.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.card.ClipCard
import com.umkt.ucopy.ui.card.ListCard
import com.umkt.ucopy.ui.card.NotebookCard
import com.umkt.ucopy.ui.card.TaskCard
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.theme.ThemeMode

@Composable
fun HomeItemCard(
    item: Clip,
    designMode: DesignMode,
    themeMode: ThemeMode,
    viewDensity: ViewDensity,
    highlightQuery: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onLongPress: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val onTap = { if (isSelectionMode) onSelectionToggle() else onCardClick() }
    when (designMode) {
        DesignMode.DEFAULT -> ClipCard(
            item = item,
            themeMode = themeMode,
            viewDensity = viewDensity,
            highlightQuery = highlightQuery,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionToggle = onSelectionToggle,
            onLongPress = onLongPress,
            onCopy = onCopy,
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            onCardClick = onCardClick,
            onCategoryClick = onCategoryClick,
            modifier = modifier
        )
        DesignMode.NOTEBOOKS -> NotebookCard(
            item = item,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onCopy = onCopy,
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            onCategoryClick = onCategoryClick,
            onClick = onTap,
            onLongClick = onLongPress,
            viewDensity = viewDensity,
            modifier = modifier
        )
        DesignMode.LIST -> ListCard(
            item = item,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onCopy = onCopy,
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            onCategoryClick = onCategoryClick,
            onClick = onTap,
            onLongClick = onLongPress,
            viewDensity = viewDensity,
            modifier = modifier
        )
        DesignMode.TASK -> TaskCard(
            item = item,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onCopy = onCopy,
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            onCategoryClick = onCategoryClick,
            onClick = onTap,
            onLongClick = onLongPress,
            viewDensity = viewDensity
        )
    }
}
