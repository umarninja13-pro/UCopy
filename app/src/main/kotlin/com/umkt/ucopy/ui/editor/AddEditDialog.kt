package com.umkt.ucopy.ui.editor

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.utils.AITagAnalyzer
import kotlinx.coroutines.delay
import com.umkt.ucopy.ui.state.ClipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDialog(
    item: Clip?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String?) -> Unit,
    viewModel: ClipViewModel? = null
) {
    val context = LocalContext.current
    val analyzer = remember { AITagAnalyzer(context) }

    var title by remember { mutableStateOf(item?.title ?: "") }
    var content by remember { mutableStateOf(item?.content ?: "") }
    var showLabelSheet by remember { mutableStateOf(false) }
    var isAiSuggesting by remember { mutableStateOf(false) }
    var showTitleInput by remember { mutableStateOf(item?.title?.isNotEmpty() == true) }

    val initialLabels = remember { item?.category?.split(", ")?.filter { it.isNotBlank() } ?: emptyList() }
    val selectedLabels = remember { mutableStateListOf<String>().apply { addAll(initialLabels) } }

    var suggestedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestedTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(content) {
        val isNewOrChanged = item == null || content != item.content
        if (content.isNotBlank() && content.length > 20 && isNewOrChanged) {
            isAiSuggesting = true
            delay(600)
            val suggestions = analyzer.analyzeText(content)
            suggestedCategories = suggestions.suggestedCategories
            suggestedTitle = suggestions.suggestedTitle
            isAiSuggesting = false
        } else {
            suggestedCategories = emptyList()
            suggestedTitle = null
        }
    }

    LaunchedEffect(title, suggestedTitle) {
        if (suggestedTitle != null || title.isNotEmpty()) {
            showTitleInput = true
        }
    }

    fun handleSave() {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (item == null && trimmedTitle.isBlank() && trimmedContent.isBlank()) {
            onDismiss()
            return
        }
        val finalCategory = if (selectedLabels.isEmpty()) null else selectedLabels.joinToString(", ")
        onSave(trimmedTitle, trimmedContent, finalCategory)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                
                // AI Co-Pilot Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.ai_copilot), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isAiSuggesting) {
                            Text(stringResource(R.string.analyzing_note), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        } else if (suggestedTitle != null || suggestedCategories.isNotEmpty()) {
                            Text(stringResource(R.string.here_are_suggestions), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Vertical Layout for Suggestions
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Title Suggestion
                                val currentSuggestedTitle = suggestedTitle
                                if (currentSuggestedTitle != null && title != currentSuggestedTitle) {
                                    SuggestionChip(
                                        onClick = { title = currentSuggestedTitle },
                                        label = { Text(stringResource(R.string.title_suggestion_format, currentSuggestedTitle), maxLines = 1, color = Color.Black) },
                                        icon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
                                    )
                                }
                                
                                // Label Suggestions
                                if (suggestedCategories.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    ) {
                                        suggestedCategories.filter { !selectedLabels.contains(it) }.forEach { cat ->
                                            SuggestionChip(
                                                onClick = { selectedLabels.add(cat) },
                                                label = { Text(stringResource(R.string.label_suggestion_format, cat), maxLines = 1, color = Color.Black) },
                                                icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(stringResource(R.string.ai_placeholder), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }

                // Main Content Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showTitleInput) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(stringResource(R.string.title)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(stringResource(R.string.content_label)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 250.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = Int.MAX_VALUE
                    )
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Labels & Add Title
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedLabels.forEach { label ->
                            InputChip(
                                selected = true,
                                onClick = { selectedLabels.remove(label) },
                                label = { Text(label) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = { showLabelSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Add Label")
                        }
                        
                        if (!showTitleInput) {
                            TextButton(onClick = { showTitleInput = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.title))
                            }
                        }
                    }

                    // Right Side: Actions
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { handleSave() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Label Picker Bottom Sheet
    if (showLabelSheet) {
        var newLabelText by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showLabelSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.labels), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                                    viewModel?.addLabel(trimmed)
                                    if (!selectedLabels.contains(trimmed)) {
                                        selectedLabels.add(trimmed)
                                    }
                                    newLabelText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))

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
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showLabelSheet = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
