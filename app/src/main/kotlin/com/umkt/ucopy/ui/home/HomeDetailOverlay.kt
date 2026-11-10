package com.umkt.ucopy.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.R
import com.umkt.ucopy.ui.detail.DetailScreen
import com.umkt.ucopy.ui.design.ActionPreferences
import com.umkt.ucopy.ui.shareItem
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeDetailOverlay(
    selectedItem: Clip?,
    clipToOpen: Clip?,
    actionPrefs: ActionPreferences,
    themeMode: ThemeMode,
    categories: List<String>,
    viewModel: ClipViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val isDirectLaunch = remember { clipToOpen != null }

    AnimatedVisibility(
        visible = selectedItem != null,
        enter = if (isDirectLaunch || !actionPrefs.showOpeningAnimation) {
            EnterTransition.None
        } else {
            scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 150))
        },
        exit = ExitTransition.None
    ) {
        var lastSelectedItem by remember { mutableStateOf<Clip?>(null) }
        LaunchedEffect(selectedItem) {
            if (selectedItem != null) lastSelectedItem = selectedItem
        }

        val currentSelected = selectedItem ?: lastSelectedItem
        if (currentSelected != null) {
            val isNewBlank = currentSelected.id.isEmpty()
            if (isNewBlank) {
                DetailScreen(
                    item = currentSelected,
                    themeMode = themeMode,
                    onDismiss = onDismiss,
                    onCopy = { },
                    onShare = { },
                    onEdit = { title, content, category, todoItems ->
                        viewModel.addOrUpdateItem(title, content, category = category, todoItems = todoItems, isTodo = currentSelected.isTodo)
                        onDismiss()
                    },
                    onDelete = onDismiss,
                    onPinToggle = { },
                    categories = categories,
                    initialEditing = true,
                    viewModel = viewModel
                )
            } else {
                DetailScreen(
                    item = currentSelected,
                    themeMode = themeMode,
                    onDismiss = onDismiss,
                    onCopy = { viewModel.copyToClipboard(currentSelected.content) },
                    onShare = { shareItem(context, currentSelected) },
                    onEdit = { title, content, category, todoItems ->
                        viewModel.addOrUpdateItem(
                            title,
                            content,
                            currentSelected.id,
                            category = category,
                            todoItems = todoItems,
                            isTodo = currentSelected.isTodo,
                            showSnackbar = false
                        )
                        onDismiss()
                    },
                    onAutoSave = { title, content, category, todoItems ->
                        val validTodos = todoItems.filter { it.title.isNotBlank() }
                        viewModel.addOrUpdateItem(
                            title,
                            content,
                            currentSelected.id,
                            category = category,
                            todoItems = validTodos,
                            isTodo = currentSelected.isTodo,
                            showSnackbar = false
                        )
                    },
                    onDelete = {
                        viewModel.deleteItem(currentSelected.id)
                        onDismiss()
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_item_deleted))
                        }
                    },
                    onPinToggle = { viewModel.togglePin(currentSelected.id) },
                    categories = categories,
                    viewModel = viewModel
                )
            }
        }
    }
}