package com.umkt.ucopy.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.LocalActionPreferences
import java.text.SimpleDateFormat
import java.util.*
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.markdown.MarkdownText
import com.umkt.ucopy.ui.label.LabelChips

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    item: Clip,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onCopy: () -> Unit = {},
    onShare: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPinToggle: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewDensity: ViewDensity = ViewDensity.COMFORTABLE
) {
    val actionPrefs = LocalActionPreferences.current
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(item.timestamp))
    
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    val isCompact = viewDensity == ViewDensity.COMPACT
    val isGrid = viewDensity == ViewDensity.GRID
    val isDetailed = viewDensity == ViewDensity.DETAILED

    val padding = when { isCompact -> 12.dp; isGrid -> 8.dp; isDetailed -> 24.dp; else -> 16.dp }
    val titleStyle = when { isCompact -> MaterialTheme.typography.titleSmall; isGrid -> MaterialTheme.typography.titleSmall; isDetailed -> MaterialTheme.typography.titleLarge; else -> MaterialTheme.typography.titleMedium }
    val previewMaxLines = when { isCompact -> 1; isGrid -> 2; isDetailed -> 6; else -> 4 }

    val stickyColorsLight = listOf(
        Color(0xFFFEFFC4), Color(0xFFC4F0D2), Color(0xFFC4E3FF), Color(0xFFFFD6D6),
        Color(0xFFE8C4FF), Color(0xFFFFE4C4), Color(0xFFC4FFF4), Color(0xFFFFC4E1)
    )
    val stickyColorsDark = listOf(
        Color(0xFF423F22), Color(0xFF223D2C), Color(0xFF223544), Color(0xFF422828),
        Color(0xFF3A223D), Color(0xFF423322), Color(0xFF22403B), Color(0xFF422239)
    )
    
    val colors = if (isLightTheme) stickyColorsLight else stickyColorsDark
    val colorIndex = Math.floorMod(item.id.hashCode(), colors.size)
    val stickyColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else colors[colorIndex]
    
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else if (isLightTheme) Color(0xFF202020) else Color(0xFFE0E0E0)

    val rotation = if (isSelectionMode || isGrid) 0f else {
        val hash = item.id.hashCode()
        if (hash % 2 == 0) 1.2f else -1.2f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isGrid) 8.dp else 24.dp, vertical = 12.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = stickyColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(padding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSelectionMode) {
                        Text(
                            text = formattedDate.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (item.category != null && item.category.isNotBlank()) {
                        LabelChips(
                            categoryString = item.category,
                            onCategoryClick = onCategoryClick,
                            maxVisible = 3,
                            modifier = Modifier.weight(1f, fill = false),
                            containerColor = textColor.copy(alpha = 0.1f),
                            contentColor = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 12.dp))

                Text(
                    text = item.title.ifEmpty { stringResource(R.string.untitled_note) },
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (actionPrefs.htmlPreview && (item.content.contains("<html", ignoreCase = true) || item.content.contains("<style", ignoreCase = true))) {
                    HtmlCardView(
                        htmlContent = item.content,
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else if (actionPrefs.markdownPreview) {
                    MarkdownText(
                        markdown = item.content.take(if (isGrid) 60 else 2000).ifEmpty { stringResource(R.string.no_content) },
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = previewMaxLines,
                        modifier = Modifier.fillMaxWidth(),
                        makeLinksClickable = false
                    )
                } else {
                    Text(
                        text = item.content.take(120).replace("\n", " ").ifEmpty { stringResource(R.string.no_content) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = previewMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                    )
                }

                if ((actionPrefs.showCopy || actionPrefs.showShare) && !isCompact && !isGrid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (actionPrefs.showCopy) {
                            IconButton(onClick = onCopy, enabled = !isSelectionMode, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, stringResource(R.string.copy), tint = if (isSelectionMode) textColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (actionPrefs.showShare) {
                            IconButton(onClick = onShare, enabled = !isSelectionMode, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, stringResource(R.string.share), tint = if (isSelectionMode) textColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        
        if (!isSelected && !isGrid && !isCompact) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .width(90.dp)
                    .height(24.dp)
                    .background(
                        color = if (isLightTheme) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .shadow(2.dp, RoundedCornerShape(2.dp))
            )
        }
    }
}
