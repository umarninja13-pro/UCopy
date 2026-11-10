package com.umkt.ucopy.ui.label

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDialog(
    category: String?,
    categories: List<String>,
    onCategoryChange: (String?) -> Unit,
    onCreateLabel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedLabels = remember { 
        mutableStateListOf<String>().apply {
            if (!category.isNullOrBlank()) {
                addAll(category.split(", ").filter { it.isNotBlank() })
            }
        }
    }
    var newLabelText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                text = stringResource(R.string.labels),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newLabelText,
                onValueChange = { newLabelText = it },
                placeholder = { Text(stringResource(R.string.labels_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (newLabelText.isNotBlank()) {
                        IconButton(onClick = {
                            val trimmed = newLabelText.trim()
                            if (trimmed.isNotEmpty()) {
                                onCreateLabel(trimmed)
                                if (!selectedLabels.contains(trimmed)) {
                                    selectedLabels.add(trimmed)
                                }
                                newLabelText = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty() && selectedLabels.isEmpty()) {
                Text(stringResource(R.string.no_labels_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allLabels = (categories + selectedLabels).distinct()
                    items(allLabels) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (selectedLabels.contains(cat)) selectedLabels.remove(cat) else selectedLabels.add(cat)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedLabels.contains(cat), 
                                onCheckedChange = { 
                                    if (selectedLabels.contains(cat)) selectedLabels.remove(cat) else selectedLabels.add(cat)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onCategoryChange(if (selectedLabels.isEmpty()) null else selectedLabels.joinToString(", "))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
            }
        }
    }
}