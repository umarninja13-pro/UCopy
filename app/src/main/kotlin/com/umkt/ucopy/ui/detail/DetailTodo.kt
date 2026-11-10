package com.umkt.ucopy.ui.detail

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.umkt.ucopy.model.Task

@Composable
fun TodoChecklist(
    todoItems: List<Task>,
    onUpdate: (List<Task>) -> Unit,
    isEditing: Boolean,
    onEditRequest: () -> Unit = {},
    focusTaskId: String? = null,
    onFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var showChecked by remember { mutableStateOf(false) }
    var newlyAddedId by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    val uncheckedItems = todoItems.filter { !it.isCompleted }
    val checkedItems = todoItems.filter { it.isCompleted }
    val currentUnchecked by rememberUpdatedState(uncheckedItems)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {

        // Unchecked Items
        itemsIndexed(currentUnchecked, key = { _, item -> item.id }) { index, item ->
            val isDragging = item.id == draggingItemId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationY = dragOffset
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = stringResource(R.string.drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingItemId = item.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val currentInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == draggingItemId } ?: return@detectDragGestures
                                    val currentCenter = currentInfo.offset + currentInfo.size / 2f + dragOffset

                                    val targetItemInfo = listState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                                        itemInfo.key != draggingItemId &&
                                        itemInfo.index < currentUnchecked.size &&
                                        currentCenter > itemInfo.offset &&
                                        currentCenter < itemInfo.offset + itemInfo.size
                                    }

                                    if (targetItemInfo != null) {
                                        val newList = currentUnchecked.toMutableList()
                                        val draggedItemIndex = newList.indexOfFirst { it.id == draggingItemId }
                                        if (draggedItemIndex != -1) {
                                            val movedItem = newList.removeAt(draggedItemIndex)
                                            newList.add(targetItemInfo.index, movedItem)
                                            onUpdate(newList + checkedItems)
                                            
                                            dragOffset += currentInfo.offset - targetItemInfo.offset
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingItemId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingItemId = null
                                    dragOffset = 0f
                                }
                            )
                        }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { checked ->
                        val newList = todoItems.map { if (it.id == item.id) it.copy(isCompleted = checked) else it }
                        onUpdate(newList)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (isEditing) {
                    var textState by remember(item.id) { mutableStateOf(item.title) }
                    val isNewItem = item.id == newlyAddedId || item.id == focusTaskId
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(isNewItem) {
                        if (isNewItem) {
                            delay(150)
                            focusRequester.requestFocus()
                            keyboardController?.show()
                            if (item.id == focusTaskId) onFocusHandled()
                            if (item.id == newlyAddedId) newlyAddedId = null
                        }
                    }

                    BasicTextField(
                        value = textState,
                        onValueChange = { newText ->
                            textState = newText
                            val newList = todoItems.map { if (it.id == item.id) it.copy(title = newText) else it }
                            onUpdate(newList)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .then(if (isNewItem) Modifier.focusRequester(focusRequester) else Modifier),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    IconButton(onClick = {
                        val newList = todoItems.filterNot { it.id == item.id }
                        onUpdate(newList)
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Text(
                        text = item.title.ifEmpty { stringResource(R.string.list_item) },
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        modifier = Modifier.weight(1f).clickable { onEditRequest() }
                    )
                }
            }
        }


        // Checked Items Section
        if (checkedItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChecked = !showChecked }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showChecked) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (showChecked) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.checked_items_format, checkedItems.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showChecked) {
                items(checkedItems, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = Color.Transparent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { checked ->
                                val newList = todoItems.map { if (it.id == item.id) it.copy(isCompleted = checked) else it }
                                onUpdate(newList)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        if (isEditing) {
                            var textState by remember(item.id) { mutableStateOf(item.title) }
                            BasicTextField(
                                value = textState,
                                onValueChange = { newText ->
                                    textState = newText
                                    val newList = todoItems.map { if (it.id == item.id) it.copy(title = newText) else it }
                                    onUpdate(newList)
                                },
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                            IconButton(onClick = {
                                val newList = todoItems.filterNot { it.id == item.id }
                                onUpdate(newList)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            Text(
                                text = item.title,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                modifier = Modifier.weight(1f).clickable { onEditRequest() }
                            )
                        }
                    }
                }
            }
        }
        // Add Item (Moved back to bottom)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEditRequest()
                        val newItem = Task(title = "")
                        newlyAddedId = newItem.id
                        onUpdate(todoItems + newItem)
                        scope.launch {
                            delay(150)
                            listState.animateScrollToItem(uncheckedItems.size)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_item_desc),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp, end = 24.dp)
                )
                Text(
                    text = stringResource(R.string.list_item),
                    style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}
