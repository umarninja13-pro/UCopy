package com.umkt.ucopy.ui.fab

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.R
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.LocalDesignConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.umkt.ucopy.model.Task

internal data class FabMenuAnimConfig(
    val staggerMs: Long,
    val enterSpec: AnimationSpec<Float>,
    val exitSpec: AnimationSpec<Float>,
    val initialScale: Float,
    val initialTranslateYDp: Dp
)

@Composable
fun FloatingActionMenu(
    onBlankNote: () -> Unit,
    onQuickNote: () -> Unit,
    onTodoList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val design = LocalDesignConfig.current
    var expanded by remember { mutableStateOf(false) }
    var isMenuVisible by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    val animConfig = remember(design.mode) {
        when (design.mode) {
            DesignMode.DEFAULT -> FabMenuAnimConfig(
                staggerMs = 50L,
                enterSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
                exitSpec = tween(200, easing = FastOutLinearInEasing),
                initialScale = 0.5f,
                initialTranslateYDp = 24.dp
            )
            DesignMode.NOTEBOOKS -> FabMenuAnimConfig(
                staggerMs = 70L,
                enterSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                exitSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing),
                initialScale = 0.7f,
                initialTranslateYDp = 16.dp
            )
            DesignMode.LIST -> FabMenuAnimConfig(
                staggerMs = 40L,
                enterSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                exitSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
                initialScale = 0.8f,
                initialTranslateYDp = 8.dp
            )
            DesignMode.TASK -> FabMenuAnimConfig(
                staggerMs = 50L,
                enterSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
                exitSpec = tween(200, easing = FastOutLinearInEasing),
                initialScale = 0.6f,
                initialTranslateYDp = 20.dp
            )
        }
    }

    val scrimAlpha = remember { Animatable(0f) }
    val itemProgress = remember { List(3) { Animatable(0f) } }

    LaunchedEffect(expanded) {
        if (expanded) {
            isMenuVisible = true
            launch { scrimAlpha.animateTo(1f, tween(250)) }
            itemProgress.forEachIndexed { index, anim ->
                launch {
                    delay(index * animConfig.staggerMs)
                    anim.animateTo(1f, animConfig.enterSpec)
                }
            }
        } else {
            launch { scrimAlpha.animateTo(0f, tween(200)) }
            itemProgress.forEachIndexed { index, anim ->
                launch {
                    val reverseIndex = itemProgress.size - 1 - index
                    delay(reverseIndex * animConfig.staggerMs)
                    anim.animateTo(0f, animConfig.exitSpec)
                }
            }
            delay(250L)
            if (!expanded) {
                isMenuVisible = false
            }
        }
    }

    val scrimColor = Color.Black.copy(alpha = 0.4f * scrimAlpha.value)

    Box(modifier = modifier.fillMaxSize()) {
        
        // Scrim (Clickable to dismiss)
        if (isMenuVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = false }
            )
        }

        // FAB and Menu Items container
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp) // Standard Scaffold padding for FAB
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Menu Items
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 80.dp) // Space for the FAB
            ) {
                if (design.mode == DesignMode.TASK) {
                    AnimatedMenuItem(itemProgress[0].value, animConfig) {
                        StickyMenuButton(
                            icon = Icons.Default.AutoAwesome,
                            text = stringResource(R.string.quick_task_ai),
                            bgColor = Color(0xFFFFD6D6),
                            onClick = { expanded = false; onQuickNote() }
                        )
                    }
                    AnimatedMenuItem(itemProgress[1].value, animConfig) {
                        StickyMenuButton(
                            icon = Icons.Default.Create,
                            text = stringResource(R.string.blank_task),
                            bgColor = Color(0xFFC4E3FF),
                            onClick = { expanded = false; onBlankNote() }
                        )
                    }
                    AnimatedMenuItem(itemProgress[2].value, animConfig) {
                        StickyMenuButton(
                            icon = Icons.Default.CheckCircle,
                            text = stringResource(R.string.todo_list),
                            bgColor = Color(0xFFC4F0D2),
                            onClick = { expanded = false; onTodoList() }
                        )
                    }
                } else {
                    AnimatedMenuItem(itemProgress[0].value, animConfig) {
                        KeepStyleMenuButton(
                            icon = Icons.Default.AutoAwesome,
                            text = stringResource(R.string.quick_note_ai),
                            onClick = { expanded = false; onQuickNote() }
                        )
                    }
                    AnimatedMenuItem(itemProgress[1].value, animConfig) {
                        KeepStyleMenuButton(
                            icon = Icons.Default.CheckCircle,
                            text = stringResource(R.string.todo_list),
                            onClick = { expanded = false; onTodoList() }
                        )
                    }
                    AnimatedMenuItem(itemProgress[2].value, animConfig) {
                        KeepStyleMenuButton(
                            icon = Icons.Default.Create,
                            text = stringResource(R.string.blank_note),
                            onClick = { expanded = false; onBlankNote() }
                        )
                    }
                }
            }

            // The ONE AND ONLY FAB
            when (design.mode) {
                DesignMode.NOTEBOOKS -> {
                    ExtendedFloatingActionButton(
                        onClick = { expanded = !expanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(100.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.rotate(rotation)) },
                        text = { Text(if (expanded) stringResource(R.string.close) else stringResource(R.string.create_new), fontWeight = FontWeight.Medium) }
                    )
                }
                DesignMode.LIST -> {
                    FloatingActionButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp).rotate(rotation))
                    }
                }
                DesignMode.TASK -> {
                    FloatingActionButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xFFFEFFC4), // Sticky Yellow
                        contentColor = Color(0xFF202020)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp).rotate(rotation))
                    }
                }
                else -> {
                    FloatingActionButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp).rotate(rotation))
                    }
                }
            }
        }
    }
}