package com.umkt.ucopy.ui.detail

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.model.Task
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.LocalDesignConfig
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.utils.UndoRedoHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.markdown.MarkdownText
import com.umkt.ucopy.ui.label.LabelDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    item: Clip,
    themeMode: ThemeMode,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: (title: String, content: String, category: String?, todoItems: List<Task>) -> Unit,
    onAutoSave: (title: String, content: String, category: String?, todoItems: List<Task>) -> Unit = { t, c, cat, todos -> onEdit(t, c, cat, todos) },
    onDelete: () -> Unit,
    onPinToggle: () -> Unit,
    categories: List<String> = emptyList(),
    allTags: List<String> = emptyList(),
    initialEditing: Boolean = false,
    viewModel: ClipViewModel? = null
) {
    var title by remember { mutableStateOf(item.title) }
    var content by remember { mutableStateOf(item.content) }
    var selectedCategory by remember { mutableStateOf(item.category) }
    var isPinned by remember { mutableStateOf(item.isPinned) }
    var todoItems by remember { mutableStateOf(item.todoItems) }

    var isEditing by remember { mutableStateOf(initialEditing) }
    var showTagsDialog by remember { mutableStateOf(false) }
    var showTodoMenu by remember { mutableStateOf(false) }
    var focusTaskId by remember { mutableStateOf<String?>(null) }
    var isUndoRedoInProgress by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var viewportHeight by remember { mutableStateOf(0) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val marginBottomPx = with(density) { 100.dp.toPx() }
    val marginTopPx = with(density) { 20.dp.toPx() }

    val undoRedoHelper = remember { UndoRedoHelper(content) }
    val canUndo by undoRedoHelper.canUndo.collectAsState()
    val canRedo by undoRedoHelper.canRedo.collectAsState()
    val currentText by undoRedoHelper.text.collectAsState()

    var contentFieldValue by remember {
        mutableStateOf(TextFieldValue(currentText, selection = TextRange(currentText.length)))
    }
    var titleFieldValue by remember { mutableStateOf(TextFieldValue(title, selection = TextRange(title.length))) }

    val wordCount = remember(content) { content.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size }
    var showLargeContentWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (item.isTodo && item.id.isEmpty() && todoItems.isEmpty()) {
            val newTask = Task(title = "")
            todoItems = listOf(newTask)
            focusTaskId = newTask.id
        }
    }

    fun performSave() {
        val validTodoItems = todoItems.filter { it.title.isNotBlank() }
        if (title.isBlank() && content.isBlank() && validTodoItems.isEmpty()) {
            onDismiss()
            return
        }
        val finalCategory = if (selectedCategory.isNullOrBlank()) null else selectedCategory
        onEdit(title, content, finalCategory, validTodoItems)
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    fun saveAndDismiss() {
        val acknowledged = viewModel?.isLargeNoteAcknowledged(item.id) ?: false
        if (wordCount > 15000 && !acknowledged) {
            showLargeContentWarning = true
        } else {
            performSave()
        }
    }

    LargeNoteWarningDialog(
        show = showLargeContentWarning,
        onDismiss = { showLargeContentWarning = false },
        onConfirm = {
            showLargeContentWarning = false
            viewModel?.acknowledgeLargeNote(item.id)
            performSave()
        }
    )

    BackHandler {
        saveAndDismiss()
    }

    fun toggleEditMode() {
        isEditing = !isEditing
    }

    fun togglePin() {
        isPinned = !isPinned
        onPinToggle()
    }

    LaunchedEffect(titleFieldValue) {
        if (isEditing) {
            title = titleFieldValue.text
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            undoRedoHelper.reset(content)
            titleFieldValue = TextFieldValue(title, selection = TextRange(title.length))
            contentFieldValue = TextFieldValue(content, selection = TextRange(content.length))
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(currentText) {
        if (isEditing && contentFieldValue.text != currentText) {
            val oldText = contentFieldValue.text
            val newText = currentText
            
            var start = 0
            while (start < oldText.length && start < newText.length && oldText[start] == newText[start]) {
                start++
            }
            var endOld = oldText.length
            var endNew = newText.length
            while (endOld > start && endNew > start && oldText[endOld - 1] == newText[endNew - 1]) {
                endOld--
                endNew--
            }
            
            val safeCursorIndex = endNew.coerceIn(0, newText.length)
            contentFieldValue = TextFieldValue(newText, selection = TextRange(safeCursorIndex))
            content = newText
        }
    }

    LaunchedEffect(contentFieldValue, textLayoutResult, viewportHeight, isEditing) {
        if (!isEditing || isUndoRedoInProgress) return@LaunchedEffect
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (viewportHeight <= 0) return@LaunchedEffect
        if (layout.layoutInput.text.text != contentFieldValue.text) return@LaunchedEffect
        val cursorIndex = contentFieldValue.selection.end
            .coerceIn(0, contentFieldValue.text.length)
        val cursorRect = layout.getCursorRect(cursorIndex)
        val currentScroll = scrollState.value
        val viewportBottom = currentScroll + viewportHeight
        when {
            cursorRect.bottom + marginBottomPx > viewportBottom -> {
                val target = (cursorRect.bottom + marginBottomPx - viewportHeight)
                    .toInt().coerceAtLeast(0)
                if (target != currentScroll) scrollState.scrollTo(target)
            }
            cursorRect.top - marginTopPx < currentScroll -> {
                val target = (cursorRect.top - marginTopPx)
                    .toInt().coerceAtLeast(0)
                if (target != currentScroll) scrollState.scrollTo(target)
            }
        }
    }

    val design = LocalDesignConfig.current

    val titleTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = when (design.mode) {
            DesignMode.NOTEBOOKS -> 24.sp
            DesignMode.LIST -> 22.sp
            DesignMode.TASK -> 20.sp
            else -> 22.sp
        },
        fontWeight = FontWeight.Normal,
        lineHeight = 32.sp
    )

    key(themeMode) {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                DetailTopBar(
                    item = item,
                    isPinned = isPinned,
                    themeMode = themeMode,
                    designMode = design.mode,
                    showTodoMenu = showTodoMenu,
                    onShowTodoMenuChange = { showTodoMenu = it },
                    onBackClick = { saveAndDismiss() },
                    onCopy = onCopy,
                    onShare = onShare,
                    onTagsClick = { showTagsDialog = true },
                    onPinClick = { togglePin() },
                    hasCheckedItems = todoItems.any { it.isCompleted },
                    onUntickAll = {
                        todoItems = todoItems.map { it.copy(isCompleted = false) }
                        if (!isEditing) onAutoSave(title, content, selectedCategory, todoItems)
                    },
                    onDeleteTicked = {
                        todoItems = todoItems.filter { !it.isCompleted }
                        if (!isEditing) onAutoSave(title, content, selectedCategory, todoItems)
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding() // Fix for keyboard overlap
                    .background(
                        when (design.mode) {
                            DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
                            DesignMode.LIST -> MaterialTheme.colorScheme.background
                            else -> MaterialTheme.colorScheme.background
                        }
                    )
            ) {
                DetailTitleSection(
                    isEditing = isEditing,
                    titleFieldValue = titleFieldValue,
                    onTitleChange = { titleFieldValue = it },
                    title = title,
                    designMode = design.mode,
                    titleTextStyle = titleTextStyle,
                    focusRequester = focusRequester,
                    onEditClick = { isEditing = true }
                )

                if (item.isTodo) {
                    TodoChecklist(
                        todoItems = todoItems,
                        onUpdate = { 
                            todoItems = it
                            // Auto-save in read mode without closing screen
                            if (!isEditing) {
                                onAutoSave(title, content, selectedCategory, it)
                            }
                        },
                        isEditing = isEditing,
                        onEditRequest = { isEditing = true },
                        focusTaskId = focusTaskId,
                        onFocusHandled = { focusTaskId = null },
                        modifier = Modifier.weight(1f)
                    )
                } else if (isEditing) {
                    DetailContentEditor(
                        contentFieldValue = contentFieldValue,
                        onContentChange = { newValue ->
                            contentFieldValue = newValue
                            content = newValue.text
                            undoRedoHelper.setText(newValue.text)
                        },
                        onTextLayout = { result -> textLayoutResult = result },
                        scrollState = scrollState,
                        focusRequester = focusRequester,
                        content = content,
                        onGloballyPositioned = { coords -> viewportHeight = coords.size.height },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    if (content.contains("<html", ignoreCase = true) || content.contains("<style", ignoreCase = true) || content.contains("<body", ignoreCase = true)) {
                        DetailHtmlViewer(
                            content = content,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.background)
                        )
                    } else {
                        DetailMarkdownViewer(
                            content = content,
                            onClick = { isEditing = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                if (isEditing) {
                    DetailUndoRedoBar(
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = {
                            isUndoRedoInProgress = true
                            undoRedoHelper.undo()
                            coroutineScope.launch {
                                delay(150)
                                isUndoRedoInProgress = false
                            }
                        },
                        onRedo = {
                            isUndoRedoInProgress = true
                            undoRedoHelper.redo()
                            coroutineScope.launch {
                                delay(150)
                                isUndoRedoInProgress = false
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showTagsDialog) {
        LabelDialog(
            category = selectedCategory,
            categories = categories,
            onCategoryChange = { selectedCategory = it },
            onCreateLabel = { newLabel -> viewModel?.addLabel(newLabel) },
            onDismiss = { showTagsDialog = false }
        )
    }
}
