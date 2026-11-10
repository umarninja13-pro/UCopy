package com.umkt.ucopy.ui.widget

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.model.Task
import com.umkt.ucopy.ui.theme.UCopyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class WidgetAddTodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val noteId = intent.getStringExtra("noteId")
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

        if (noteId == null || appWidgetId == -1) {
            finish()
            return
        }

        setContent {
            UCopyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.6f) // Dimmed background
                ) {
                    @Suppress("DEPRECATION")
                    val onSubmit: (String) -> Unit = { taskText ->
                        val scope = CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            val repo = ClipRepository(this@WidgetAddTodoActivity)
                            val notes = repo.getAll()
                            val note = notes.find { it.id == noteId }
                            if (note != null) {
                                val newTodo = Task(id = UUID.randomUUID().toString(), title = taskText)
                                repo.update(note.copy(todoItems = note.todoItems + newTodo))
                                
                                val manager = AppWidgetManager.getInstance(this@WidgetAddTodoActivity)
                                NoteWidgetProvider.updateWidget(this@WidgetAddTodoActivity, manager, appWidgetId)
                                manager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), com.umkt.ucopy.R.id.widget_list)
                                
                                withContext(Dispatchers.Main) { finish() }
                            }
                        }
                    }

                    InlineAddTodoUI(
                        onDismiss = { finish() },
                        onSubmit = onSubmit
                    )
                }
            }
        }
    }
}

@Composable
fun InlineAddTodoUI(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Auto-focus and show keyboard when opened
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) { 
                            onSubmit(text.trim()); 
                            keyboard?.hide(); 
                            focusManager.clearFocus() 
                        }
                    }),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(stringResource(R.string.add_new_task_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                )
                IconButton(
                    onClick = { 
                        if (text.isNotBlank()) { 
                            onSubmit(text.trim()); 
                            keyboard?.hide(); 
                            focusManager.clearFocus() 
                        } 
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
