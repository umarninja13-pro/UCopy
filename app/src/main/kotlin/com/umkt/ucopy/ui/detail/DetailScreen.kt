package com.umkt.ucopy.ui.detail

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.platform.LocalContext
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
    onCopy: (text: String) -> Unit,
    onShare: (text: String) -> Unit,
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

    val contentState = remember { TextFieldState(currentText) }
    var titleFieldValue by remember { mutableStateOf(TextFieldValue(title, selection = TextRange(title.length))) }

    var showLargeContentWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (item.isTodo && item.id.isEmpty() && todoItems.isEmpty()) {
            val newTask = Task(title = "")
            todoItems = listOf(newTask)
            focusTaskId = newTask.id
        }
    }

    var lastSaved by remember {
        mutableStateOf(
            listOf<Any?>(item.title, item.content, item.category, item.todoItems.filter { it.title.isNotBlank() })
        )
    }

    fun autoSaveNow() {
        val validTodos = todoItems.filter { it.title.isNotBlank() }
        if (title.isBlank() && content.isBlank() && validTodos.isEmpty()) return
        val cat = if (selectedCategory.isNullOrBlank()) null else selectedCategory
        val snapshot = listOf<Any?>(title, content, cat, validTodos)
        if (snapshot == lastSaved) return
        lastSaved = snapshot
        onAutoSave(title, content, cat, validTodos)
    }

    val latestAutoSave by rememberUpdatedState(newValue = { autoSaveNow() })

    LaunchedEffect(title, content, selectedCategory, todoItems) {
        delay(600)
        latestAutoSave()
    }

    val activityContext = LocalContext.current
    DisposableEffect(activityContext) {
        val owner = activityContext.findComponentActivity()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                latestAutoSave()
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            latestAutoSave()
        }
    }

    fun copyText(): String =
        if (item.isTodo) {
            todoItems.filter { it.title.isNotBlank() }
                .joinToString("\n") { (if (it.isCompleted) "\u2611 " else "\u2610 ") + it.title }
        } else content

    fun performSave() {
        val validTodoItems = todoItems.filter { it.title.isNotBlank() }
        if (title.isBlank() && content.isBlank() && validTodoItems.isEmpty()) {
            onDismiss()
            return
        }
        val finalCategory = if (selectedCategory.isNullOrBlank()) null else selectedCategory
        lastSaved = listOf<Any?>(title, content, finalCategory, validTodoItems)
        onEdit(title, content, finalCategory, validTodoItems)
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    fun saveAndDismiss() {
        val acknowledged = viewModel?.isLargeNoteAcknowledged(item.id) ?: false
        val wordCount = if (content.length > 60000) content.trim().split(Regex("\\s+")).count { it.isNotEmpty() } else 0
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
            contentState.edit {
                replace(0, length, content)
                selection = TextRange(content.length)
            }
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(currentText) {
        if (isEditing && contentState.text.toString() != currentText) {
            val oldText = contentState.text.toString()
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
            contentState.edit {
                replace(0, length, newText)
                selection = TextRange(safeCursorIndex)
            }
            content = newText
        }
    }

    LaunchedEffect(contentState) {
        snapshotFlow { contentState.text.toString() }.collect { text ->
            if (isEditing && text != content) {
                content = text
                undoRedoHelper.setText(text)
            }
        }
    }

    val layoutHolder = remember { arrayOfNulls<TextLayoutResult>(1) }
    val viewportHolder = remember { IntArray(1) }
    var autoScrollTick by remember { mutableIntStateOf(0) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    var autoScrollUntil by remember { mutableLongStateOf(0L) }

    LaunchedEffect(contentState) {
        var previous = contentState.selection
        snapshotFlow { contentState.selection }.collect { selection ->
            val old = previous
            previous = selection
            val layout = layoutHolder[0]
            val viewport = viewportHolder[0]
            if (!isEditing || layout == null || viewport <= 0 || selection.collapsed || old.collapsed) return@collect
            val moved = when {
                selection.end != old.end && selection.start == old.start -> selection.end
                selection.start != old.start && selection.end == old.end -> selection.start
                else -> -1
            }
            if (moved < 0 || moved > layout.layoutInput.text.length) return@collect
            val rect = layout.getCursorRect(moved)
            val edgePx = with(density) { 96.dp.toPx() }
            val topV = rect.top - scrollState.value
            val bottomV = rect.bottom - scrollState.value
            val speed = when {
                bottomV > viewport - edgePx ->
                    (2f + 8f * ((bottomV - (viewport - edgePx)) / edgePx).coerceIn(0f, 1.5f)) * density.density
                topV < edgePx ->
                    -(2f + 8f * ((edgePx - topV) / edgePx).coerceIn(0f, 1.5f)) * density.density
                else -> 0f
            }
            if (speed != 0f) {
                autoScrollSpeed = speed
                autoScrollUntil = System.currentTimeMillis() + 500
                autoScrollTick++
            }
        }
    }

    LaunchedEffect(autoScrollTick) {
        while (System.currentTimeMillis() < autoScrollUntil) {
            withFrameNanos { }
            scrollState.scrollBy(autoScrollSpeed)
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
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp
    )

    val isHtmlContent = content.contains("<html", ignoreCase = true) || content.contains("<style", ignoreCase = true) || content.contains("<body", ignoreCase = true)

    key(themeMode) {
        Scaffold(modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                DetailTopBar(
                    item = item,
                    isPinned = isPinned,
                    themeMode = themeMode,
                    designMode = design.mode,
                    showTodoMenu = showTodoMenu,
                    onShowTodoMenuChange = { showTodoMenu = it },
                    onBackClick = { saveAndDismiss() },
                    onCopy = { onCopy(copyText()) },
                    onShare = { onShare(copyText()) },
                    onTagsClick = { showTagsDialog = true },
                    onPinClick = { togglePin() },
                    hasCheckedItems = todoItems.any { it.isCompleted },
                    onUntickAll = {
                        todoItems = todoItems.map { it.copy(isCompleted = false) }
                        autoSaveNow()
                    },
                    onDeleteTicked = {
                        todoItems = todoItems.filter { !it.isCompleted }
                        autoSaveNow()
                    },
                    isEditing = isEditing,
                    onToggleEdit = {
                        if (isEditing) autoSaveNow()
                        isEditing = !isEditing
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(if (isEditing || item.isTodo || isHtmlContent) Modifier.navigationBarsPadding() else Modifier)
                    .imePadding() // Fix for keyboard overlap
                    .background(
                        when (design.mode) {
                            DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
                            DesignMode.LIST -> MaterialTheme.colorScheme.background
                            else -> MaterialTheme.colorScheme.background
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .background(
                            when (design.mode) {
                                DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
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
                }

                if (item.isTodo) {
                    TodoChecklist(
                        todoItems = todoItems,
                        onUpdate = { 
                            todoItems = it
                            autoSaveNow()
                        },
                        isEditing = isEditing,
                        onEditRequest = { isEditing = true },
                        focusTaskId = focusTaskId,
                        onFocusHandled = { focusTaskId = null },
                        modifier = Modifier.weight(1f)
                    )
                } else if (isEditing) {
                    DetailContentEditor(
                        state = contentState,
                        scrollState = scrollState,
                        focusRequester = focusRequester,
                        content = content,
                        onTextLayout = { layoutHolder[0] = it },
                        onViewportHeight = { viewportHolder[0] = it },
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

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
