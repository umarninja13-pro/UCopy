package com.umkt.ucopy.ui.card

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.CardStyle
import com.umkt.ucopy.ui.design.LocalActionPreferences
import com.umkt.ucopy.ui.design.LocalDesignConfig
import com.umkt.ucopy.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.*
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.markdown.MarkdownText
import com.umkt.ucopy.ui.label.LabelChips

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipCard(
    item: Clip,
    themeMode: ThemeMode,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    highlightQuery: String = "",
    viewDensity: ViewDensity = ViewDensity.COMFORTABLE,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val design = LocalDesignConfig.current
    val actionPrefs = LocalActionPreferences.current

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    val isCompact = viewDensity == ViewDensity.COMPACT
    val isGrid = viewDensity == ViewDensity.GRID
    val isDetailed = viewDensity == ViewDensity.DETAILED

    val cardPadding = when { isCompact -> 12.dp; isGrid -> 12.dp; isDetailed -> 28.dp; else -> 20.dp }
    val titleStyle = when { isCompact || isGrid -> MaterialTheme.typography.titleSmall; isDetailed -> MaterialTheme.typography.headlineSmall; else -> MaterialTheme.typography.titleLarge }
    val previewMaxLines = when { isCompact || isGrid -> 2; isDetailed -> 6; else -> 4 }
    val iconSize = when { isCompact || isGrid -> 16.dp; isDetailed -> 24.dp; else -> 20.dp }
    val buttonSize = when { isCompact || isGrid -> 28.dp; isDetailed -> 40.dp; else -> 36.dp }
    val horizontalPadding = when { isCompact -> 12.dp; isGrid -> 8.dp; isDetailed -> 16.dp; else -> 16.dp }
    val verticalPadding = when { isCompact -> 4.dp; isGrid -> 4.dp; isDetailed -> 12.dp; else -> 8.dp }

    val cornerRadius = if (design.cardStyle == CardStyle.BORDERLESS) 0.dp else design.cardShape
    val elevation = when (design.cardStyle) { CardStyle.PAPER -> 2.dp; else -> 0.dp }

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            design.mode == com.umkt.ucopy.ui.design.DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
            design.cardStyle == CardStyle.TONAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = design.cardAlpha)
            design.cardStyle == CardStyle.PAPER -> MaterialTheme.colorScheme.surface
            design.cardStyle == CardStyle.STICKY -> MaterialTheme.colorScheme.surfaceVariant
            design.cardStyle == CardStyle.BORDERLESS -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = design.cardAlpha)
        },
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "card_color"
    )

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = verticalPadding),
        shape = RoundedCornerShape(cornerRadius),
        border = if (design.mode == com.umkt.ucopy.ui.design.DesignMode.DEFAULT && !isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        } else if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (isSelectionMode) onSelectionToggle() else onCardClick() },
                    onLongClick = { if (!isSelectionMode) onLongPress() }
                )
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title.ifEmpty { stringResource(R.string.untitled) },
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (item.category != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LabelChips(
                        categoryString = item.category,
                        onCategoryClick = onCategoryClick,
                        maxVisible = if (isCompact || isGrid) 1 else 2
                    )
                }

                Spacer(modifier = Modifier.height(if (isDetailed) 8.dp else 4.dp))

                if (item.isTodo) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.todoItems.take(previewMaxLines).forEach { todo ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (todo.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = todo.title.ifEmpty { stringResource(R.string.list_item) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (todo.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (item.todoItems.size > previewMaxLines) {
                            Text(text = stringResource(R.string.more_items, item.todoItems.size - previewMaxLines), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 24.dp))
                        }
                    }
                } else if (actionPrefs.htmlPreview && (item.content.contains("<html", ignoreCase = true) || item.content.contains("<style", ignoreCase = true) || item.content.contains("<body", ignoreCase = true))) {
                    HtmlCardView(
                        htmlContent = item.content,
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = { if (isSelectionMode) onSelectionToggle() else onCardClick() },
                        onLongClick = { if (!isSelectionMode) onLongPress() }
                    )
                } else {
                    val previewText = item.content.take(2000)
                    if (actionPrefs.markdownPreview) {
                        key(themeMode) {
                            MarkdownText(
                                markdown = previewText.ifEmpty { stringResource(R.string.no_content) },
                                maxLines = previewMaxLines,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                highlightQuery = highlightQuery,
                                modifier = Modifier.fillMaxWidth(),
                                makeLinksClickable = false
                            )
                        }
                    } else {
                        Text(
                            text = previewText.replace("\n", " ").ifEmpty { stringResource(R.string.no_content) },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = previewMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (design.showCardDividers) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(modifier = Modifier.height(if (isDetailed) 16.dp else 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (actionPrefs.showCopy) {
                            IconButton(onClick = onCopy, enabled = !isSelectionMode, modifier = Modifier.size(buttonSize)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), tint = if (isSelectionMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                            }
                        }
                        if (actionPrefs.showShare) {
                            IconButton(onClick = onShare, enabled = !isSelectionMode, modifier = Modifier.size(buttonSize)) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = if (isSelectionMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                    Text(text = formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(end = 8.dp))
                }
            }
        }
    }
}
