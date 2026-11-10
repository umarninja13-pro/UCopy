package com.umkt.ucopy.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSelectorDialog(
    currentMode: DesignMode,
    onSelect: (DesignMode) -> Unit,
    onDismiss: () -> Unit,
    actionPrefs: ActionPreferences,
    onActionPrefsChange: (ActionPreferences) -> Unit
) {
    var showActionSettings by remember { mutableStateOf(false) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.choose_design),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.choose_design_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showActionSettings = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.design_actions_desc), tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.markdown_preview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.markdown_preview_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = actionPrefs.markdownPreview,
                    onCheckedChange = { onActionPrefsChange(actionPrefs.copy(markdownPreview = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.html_preview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.html_preview_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = actionPrefs.htmlPreview,
                    onCheckedChange = { onActionPrefsChange(actionPrefs.copy(htmlPreview = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.layout_animations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.layout_animations_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = actionPrefs.showListAnimations,
                    onCheckedChange = { onActionPrefsChange(actionPrefs.copy(showListAnimations = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.note_animation),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.note_animation_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = actionPrefs.showOpeningAnimation,
                    onCheckedChange = { onActionPrefsChange(actionPrefs.copy(showOpeningAnimation = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(DesignPresets.all()) { design ->
                    val selected = design.mode == currentMode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(design.mode)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniDesignPreview(design)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(design.displayNameRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(design.taglineRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionSettings) {
        ActionSettingsDialog(
            prefs = actionPrefs,
            onSave = onActionPrefsChange,
            onDismiss = { showActionSettings = false }
        )
    }
}

@Composable
private fun SwatchCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
    )
}


@Composable
private fun MiniDesignPreview(design: DesignConfig) {
    val shape = RoundedCornerShape(8.dp)
    val baseColor = design.lightScheme.background
    val cardColor = design.lightScheme.surface
    val primary = design.lightScheme.primary
    val outline = design.lightScheme.outline

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (design.mode) {
            DesignMode.DEFAULT -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, outline),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.padding(4.dp)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(3.dp).background(primary, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.fillMaxWidth().height(2.dp).background(outline.copy(alpha=0.5f), RoundedCornerShape(1.dp)))
                    }
                }
            }
            DesignMode.NOTEBOOKS -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, outline.copy(alpha=0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(primary))
                        Column(Modifier.padding(4.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f).height(3.dp).background(primary, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.fillMaxWidth().height(2.dp).background(outline.copy(alpha=0.5f), RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }
            DesignMode.LIST -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.fillMaxWidth(0.8f).height(3.dp).background(primary, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(outline, RoundedCornerShape(1.dp)))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.5f).height(2.dp).background(outline.copy(alpha=0.5f), RoundedCornerShape(1.dp)))
                }
            }
            DesignMode.TASK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = -4f }
                        .background(Color(0xFFFFF59D), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth(0.7f).height(3.dp).background(Color(0xFF202020).copy(alpha=0.6f), RoundedCornerShape(2.dp)))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF202020).copy(alpha=0.3f), RoundedCornerShape(1.dp)))
                    }
                }
            }
        }
    }
}
