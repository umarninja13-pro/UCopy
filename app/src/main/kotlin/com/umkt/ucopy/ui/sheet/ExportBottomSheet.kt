package com.umkt.ucopy.ui.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.export_selected_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.export_selected_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.json),
                        subtitle = stringResource(R.string.for_backup_desc),
                        icon = Icons.Default.Code,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onExport("JSON") }
                    )
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.txt),
                        subtitle = stringResource(R.string.plain_text_desc),
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onExport("TXT") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.html),
                        subtitle = stringResource(R.string.web_page_desc),
                        icon = Icons.Default.Language,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { onExport("HTML") }
                    )
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.export_md),
                        subtitle = stringResource(R.string.export_md_desc),
                        icon = Icons.AutoMirrored.Filled.Article,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onExport("MD") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.export_csv),
                        subtitle = stringResource(R.string.export_csv_desc),
                        icon = Icons.Default.TableChart,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onExport("CSV") }
                    )
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.export_png),
                        subtitle = stringResource(R.string.export_png_desc),
                        icon = Icons.Default.Image,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { onExport("PNG") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.pdf),
                        subtitle = stringResource(R.string.printable_desc),
                        icon = Icons.Default.PictureAsPdf,
                        color = MaterialTheme.colorScheme.error,
                        onClick = { onExport("PDF") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExportOption(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
