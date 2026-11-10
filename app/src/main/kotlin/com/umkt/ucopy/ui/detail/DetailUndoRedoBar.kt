package com.umkt.ucopy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

@Composable
fun DetailUndoRedoBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        RepeatableIconButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = stringResource(R.string.undo),
            enabled = canUndo,
            onClick = onUndo
        )
        RepeatableIconButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = stringResource(R.string.redo),
            enabled = canRedo,
            onClick = onRedo
        )
    }
}