package com.umkt.ucopy.ui.label

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.ui.design.LocalDesignConfig
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelEditScreen(
    labels: List<String>,
    onRename: (old: String, new: String) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    startInCreateMode: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var editingLabel by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(startInCreateMode) }
    var createText by remember { mutableStateOf("") }
    var labelToDelete by remember { mutableStateOf<String?>(null) } // For Confirmation Dialog

    val design = LocalDesignConfig.current
    val focusRequester = remember { FocusRequester() }

    // Fix for Swipe Back / Back Button closing the app
    BackHandler {
        onDismiss()
    }

    LaunchedEffect(isCreating) {
        if (isCreating) {
            delay(150)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Delete Confirmation Dialog
    if (labelToDelete != null) {
        AlertDialog(
            onDismissRequest = { labelToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_label_title)) },
            text = { Text(stringResource(R.string.delete_label_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        labelToDelete?.let { onDelete(it) }
                        labelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { labelToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_labels_title), fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // Create New Label Row
            if (isCreating) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isCreating = false; createText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                    BasicTextField(
                        value = createText,
                        onValueChange = { createText = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (createText.isEmpty()) Text(stringResource(R.string.create_new_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                    IconButton(onClick = {
                        if (createText.isNotBlank()) {
                            onCreate(createText.trim())
                            isCreating = false
                            createText = ""
                            keyboardController?.hide()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.create_new_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { isCreating = true; keyboardController?.show() }) {
                        Text(stringResource(R.string.create), fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider()
            }

            LazyColumn {
                items(labels) { label ->
                    if (editingLabel == label) {
                        // Edit Mode
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { editingLabel = null }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            BasicTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                            IconButton(onClick = { labelToDelete = label }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = {
                                if (editText.isNotBlank() && editText != label) {
                                    onRename(label, editText.trim())
                                }
                                editingLabel = null
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider()
                    } else {
                        // View Mode
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            
                            IconButton(onClick = { editingLabel = label; editText = label }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { labelToDelete = label }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
