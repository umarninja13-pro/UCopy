package com.umkt.ucopy.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.ui.graphics.Color
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    item: Clip,
    isPinned: Boolean,
    themeMode: ThemeMode,
    designMode: DesignMode,
    showTodoMenu: Boolean,
    onShowTodoMenuChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onTagsClick: () -> Unit,
    onPinClick: () -> Unit,
    hasCheckedItems: Boolean,
    onUntickAll: () -> Unit,
    onDeleteTicked: () -> Unit
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            if (item.isTodo) {
                Box {
                    IconButton(onClick = { onShowTodoMenuChange(true) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showTodoMenu,
                        onDismissRequest = { onShowTodoMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.untick_all_items)) },
                            enabled = hasCheckedItems,
                            onClick = {
                                onShowTodoMenuChange(false)
                                onUntickAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_ticked_items)) },
                            enabled = hasCheckedItems,
                            onClick = {
                                onShowTodoMenuChange(false)
                                onDeleteTicked()
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
            }
            IconButton(onClick = onTagsClick) {
                Icon(
                    Icons.AutoMirrored.Outlined.Label,
                    contentDescription = stringResource(R.string.edit_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPinClick) {
                Icon(
                    if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = when {
                themeMode == ThemeMode.AMOLED -> Color.Black
                designMode == DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
                designMode == DesignMode.LIST -> MaterialTheme.colorScheme.background
                else -> MaterialTheme.colorScheme.surface
            }
        )
    )
}