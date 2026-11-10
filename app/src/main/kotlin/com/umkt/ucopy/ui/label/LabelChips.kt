package com.umkt.ucopy.ui.label

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LabelChips(
    categoryString: String?,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 2,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (categoryString.isNullOrBlank()) return
    val labels = categoryString.split(", ").filter { it.isNotBlank() }
    if (labels.isEmpty()) return

    var showAllMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleLabels = labels.take(maxVisible)
        val remainingCount = labels.size - visibleLabels.size

        visibleLabels.forEach { label ->
            Surface(
                onClick = { onCategoryClick(label) },
                shape = RoundedCornerShape(50),
                color = containerColor,
                contentColor = contentColor
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (remainingCount > 0) {
            Box {
                Surface(
                    onClick = { showAllMenu = true },
                    shape = RoundedCornerShape(50),
                    color = containerColor,
                    contentColor = contentColor
                ) {
                    Text(
                        text = "+$remainingCount",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                DropdownMenu(
                    expanded = showAllMenu,
                    onDismissRequest = { showAllMenu = false }
                ) {
                    labels.drop(maxVisible).forEach { label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onCategoryClick(label)
                                showAllMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
