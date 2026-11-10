package com.umkt.ucopy.ui.widget

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.theme.UCopyTheme
import kotlinx.coroutines.launch

data class WidgetSettings(
    val noteId: String,
    val theme: String,
    val font: String,
    val fontFamily: String,
    val showCopy: Boolean,
    val showAdd: Boolean,
    val showExpand: Boolean,
    val showLabel: Boolean,
    val titleColor: Int,
    val bodyColor: Int
)

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        intent?.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val prefs = UserPreferences(this)
        val existingNoteId = prefs.getStringOrNull("widget_note_$appWidgetId")
        val existingTheme = prefs.getString("widget_theme_$appWidgetId", "DARK")
        val existingFont = prefs.getString("widget_font_$appWidgetId", "MEDIUM")
        val existingFontFamily = prefs.getString("widget_font_family_$appWidgetId", "DEFAULT")
        val existingShowCopy = prefs.getBoolean("widget_show_copy_$appWidgetId", true)
        val existingShowAdd = prefs.getBoolean("widget_show_add_$appWidgetId", true)
        val existingShowExpand = prefs.getBoolean("widget_show_expand_$appWidgetId", true)
        val existingShowLabel = prefs.getBoolean("widget_show_label_$appWidgetId", true)
        val existingTitleColor = prefs.getInt("widget_title_color_$appWidgetId", 0)
        val existingBodyColor = prefs.getInt("widget_body_color_$appWidgetId", 0)

        setContent {
            UCopyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ModernDropdownConfigScreen(
                        existingNoteId = existingNoteId,
                        existingTheme = existingTheme,
                        existingFont = existingFont,
                        existingFontFamily = existingFontFamily,
                        existingShowCopy = existingShowCopy,
                        existingShowAdd = existingShowAdd,
                        existingShowExpand = existingShowExpand,
                        existingShowLabel = existingShowLabel,
                        existingTitleColor = existingTitleColor,
                        existingBodyColor = existingBodyColor,
                        onSave = { settings ->
                            prefs.putString("widget_note_$appWidgetId", settings.noteId)
                            prefs.putString("widget_theme_$appWidgetId", settings.theme)
                            prefs.putString("widget_font_$appWidgetId", settings.font)
                            prefs.putString("widget_font_family_$appWidgetId", settings.fontFamily)
                            prefs.putBoolean("widget_show_copy_$appWidgetId", settings.showCopy)
                            prefs.putBoolean("widget_show_add_$appWidgetId", settings.showAdd)
                            prefs.putBoolean("widget_show_expand_$appWidgetId", settings.showExpand)
                            prefs.putBoolean("widget_show_label_$appWidgetId", settings.showLabel)
                            prefs.putInt("widget_title_color_$appWidgetId", settings.titleColor)
                            prefs.putInt("widget_body_color_$appWidgetId", settings.bodyColor)

                            val manager = AppWidgetManager.getInstance(this)
                            NoteWidgetProvider.updateWidget(this, manager, appWidgetId)
                            @Suppress("DEPRECATION")
                            manager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), com.umkt.ucopy.R.id.widget_list)

                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDropdownConfigScreen(
    existingNoteId: String?,
    existingTheme: String,
    existingFont: String,
    existingFontFamily: String,
    existingShowCopy: Boolean,
    existingShowAdd: Boolean,
    existingShowExpand: Boolean,
    existingShowLabel: Boolean,
    existingTitleColor: Int,
    existingBodyColor: Int,
    onSave: (WidgetSettings) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf<List<Clip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedNoteId by remember { mutableStateOf<String?>(existingNoteId) }
    
    var selectedTheme by remember { mutableStateOf(existingTheme) }
    var selectedFont by remember { mutableStateOf(existingFont) }
    var selectedFontFamily by remember { mutableStateOf(existingFontFamily) }
    var showCopy by remember { mutableStateOf(existingShowCopy) }
    var showAdd by remember { mutableStateOf(existingShowAdd) }
    var showExpand by remember { mutableStateOf(existingShowExpand) }
    var showLabel by remember { mutableStateOf(existingShowLabel) }
    var titleColor by remember { mutableStateOf(existingTitleColor) }
    var bodyColor by remember { mutableStateOf(existingBodyColor) }

    val scope = rememberCoroutineScope()
    
    val colors = listOf(
        Color.White, Color(0xFFF9FAFB), Color(0xFFF3F4F6),
        Color(0xFFE5E7EB), Color(0xFFD1D5DB), Color(0xFF9CA3AF),
        Color(0xFF6B7280), Color(0xFF4B5563), Color(0xFF374151),
        Color(0xFF1F2937), Color(0xFF111827), Color.Black,
        Color(0xFFF87171), Color(0xFFEF4444), Color(0xFFDC2626),
        Color(0xFFFB923C), Color(0xFFF97316), Color(0xFFEA580C),
        Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706),
        Color(0xFFA3E635), Color(0xFF84CC16), Color(0xFF65A30D),
        Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF16A34A),
        Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669),
        Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF0284C7),
        Color(0xFF60A5FA), Color(0xFF3B82F6), Color(0xFF2563EB),
        Color(0xFF818CF8), Color(0xFF6366F1), Color(0xFF4F46E5),
        Color(0xFFA78BFA), Color(0xFF8B5CF6), Color(0xFF7C3AED),
        Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFDB2777)
    )

    LaunchedEffect(Unit) {
        scope.launch {
            val repo = ClipRepository(context)
            notes = repo.getAll()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        selectedNoteId?.let {
                            onSave(
                                WidgetSettings(
                                    noteId = it,
                                    theme = selectedTheme,
                                    font = selectedFont,
                                    fontFamily = selectedFontFamily,
                                    showCopy = showCopy,
                                    showAdd = showAdd,
                                    showExpand = showExpand,
                                    showLabel = showLabel,
                                    titleColor = titleColor,
                                    bodyColor = bodyColor
                                )
                            )
                        }
                    },
                    enabled = selectedNoteId != null,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.save_widget), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.live_preview), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AppStyleWidgetPreview(
                        theme = selectedTheme,
                        font = selectedFont,
                        fontFamily = selectedFontFamily,
                        showCopy = showCopy,
                        showAdd = showAdd,
                        showExpand = showExpand,
                        showLabel = showLabel,
                        titleColor = titleColor,
                        bodyColor = bodyColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Compact Dropdowns in 1 Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeDropdown(selectedTheme, { selectedTheme = it }, Modifier.weight(1f))
                        FontDropdown(selectedFont, { selectedFont = it }, Modifier.weight(1f))
                        FontFamilyDropdown(selectedFontFamily, { selectedFontFamily = it }, Modifier.weight(1f))
                    }
                    
                    if (selectedTheme == "TRANSPARENT") {
                        Text(
                            text = "Note: Background will be fully transparent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.visible_elements), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            CompactToggle(stringResource(R.string.copy), Icons.Default.ContentCopy, showCopy) { showCopy = it }
                            CompactToggle(stringResource(R.string.expand), Icons.Default.OpenInFull, showExpand) { showExpand = it }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            CompactToggle(stringResource(R.string.add), Icons.Default.Add, showAdd) { showAdd = it }
                            CompactToggle("U Copy", Icons.Default.TextFields, showLabel) { showLabel = it }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.title_color), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    ColorPickerRow(colors, titleColor) { titleColor = it.toArgb() }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(stringResource(R.string.paragraph_color), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    ColorPickerRow(colors, bodyColor) { bodyColor = it.toArgb() }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.select_note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_notes_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            GridNoteCard(
                                note = note,
                                isSelected = note.id == selectedNoteId,
                                onClick = { selectedNoteId = note.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactToggle(label: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 2.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ColorPickerRow(colors: List<Color>, selected: Int, onSelect: (Color) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == color.toArgb()) 3.dp else 1.dp,
                        color = if (selected == color.toArgb()) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}
