package com.umkt.ucopy.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.theme.UCopyTheme
import kotlinx.coroutines.launch

@Composable
fun GridNoteCard(note: Clip, isSelected: Boolean, onClick: () -> Unit) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    // Google Keep style background colors
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (note.isPinned) {
        if (isLightTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    } else if (note.isTodo) {
        if (isLightTheme) Color(0xFFE8F5E9) else Color(0xFF1B2E1C) // Soft Green for Todo
    } else {
        if (isLightTheme) Color(0xFFFFFDE7) else Color(0xFF2C2A1A) // Soft Yellow for Notes
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.isPinned) {
                Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.pinned), modifier = Modifier.size(16.dp).padding(bottom = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Text(
                text = note.title.ifEmpty { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (note.isTodo) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.todoItems.take(3).forEach { todo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (todo.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = todo.title.ifEmpty { stringResource(R.string.list_item) },
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (note.todoItems.size > 3) {
                        Text(stringResource(R.string.more_items, note.todoItems.size - 3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                Text(
                    text = note.content.take(80),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
