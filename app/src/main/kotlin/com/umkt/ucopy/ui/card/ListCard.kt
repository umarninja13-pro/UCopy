package com.umkt.ucopy.ui.card

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.LocalActionPreferences
import java.text.SimpleDateFormat
import java.util.*
import com.umkt.ucopy.ui.markdown.MarkdownText
import com.umkt.ucopy.ui.label.LabelChips

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListCard(
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
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(item.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
Text(
                text = formattedDate.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = item.title.ifEmpty { stringResource(R.string.untitled) },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.category != null && item.category.isNotBlank()) {
                LabelChips(
                    categoryString = item.category,
                    onCategoryClick = onCategoryClick,
                    maxVisible = 2
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (actionPrefs.showCopy || actionPrefs.showShare) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}
