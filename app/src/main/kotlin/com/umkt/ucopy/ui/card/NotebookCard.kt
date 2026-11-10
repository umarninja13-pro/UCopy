package com.umkt.ucopy.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.LocalActionPreferences
import java.text.SimpleDateFormat
import java.util.*
import com.umkt.ucopy.ui.markdown.MarkdownText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotebookCard(
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
    modifier: Modifier = Modifier
) {
    val actionPrefs = LocalActionPreferences.current
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val spineColorsLight = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFF44336), Color(0xFFFF9800)
    )
    val spineColorsDark = listOf(
        Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFE57373), Color(0xFFFFB74D)
    )
    
    val colorIndex = Math.floorMod(item.id.hashCode(), spineColorsLight.size)
    val spineColor = if (isLightTheme) spineColorsLight[colorIndex] else spineColorsDark[colorIndex]

    val cardBgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (isLightTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(item.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = if (isLightTheme && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLightTheme) 1.dp else 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(spineColor)
            )

            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title.ifEmpty { stringResource(R.string.untitled) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (actionPrefs.htmlPreview && (item.content.contains("<html", ignoreCase = true) || item.content.contains("<style", ignoreCase = true))) {
                    HtmlCardView(
                        htmlContent = item.content,
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else if (actionPrefs.markdownPreview) {
                    MarkdownText(
                        markdown = item.content.take(2000).ifEmpty { stringResource(R.string.no_content) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        makeLinksClickable = false
                    )
                } else {
                    Text(
                        text = item.content.take(150).replace("\n", " ").ifEmpty { stringResource(R.string.no_content) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())
                    ) {
                        if (item.category != null && item.category.isNotBlank()) {
                            val labels = item.category.split(", ").filter { it.isNotBlank() }
                            labels.take(2).forEach { label ->
                                Surface(
                                    onClick = { onCategoryClick(label) },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.End) {
                        if (actionPrefs.showCopy) {
                            IconButton(onClick = onCopy, enabled = !isSelectionMode, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, stringResource(R.string.copy), tint = if (isSelectionMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (actionPrefs.showShare) {
                            IconButton(onClick = onShare, enabled = !isSelectionMode, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, stringResource(R.string.share), tint = if (isSelectionMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
