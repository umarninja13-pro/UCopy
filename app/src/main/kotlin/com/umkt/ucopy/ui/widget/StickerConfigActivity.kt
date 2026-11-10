package com.umkt.ucopy.ui.widget

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.ui.theme.UCopyTheme

class StickerConfigActivity : ComponentActivity() {
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
        
        val initText = prefs.getString("sticker_text_$appWidgetId", "U Copy")
        val initTheme = prefs.getString("sticker_theme_$appWidgetId", "LIGHT")
        val initTextColor = prefs.getInt("sticker_text_color_$appWidgetId", Color.parseColor("#4F46E5"))
        val initFontSize = prefs.getString("sticker_font_size_$appWidgetId", "MEDIUM")
        val initFontFamily = prefs.getString("sticker_font_family_$appWidgetId", "DEFAULT")
        val initShadow = prefs.getInt("sticker_shadow_radius_$appWidgetId", 4)
        val initStrokeWidth = prefs.getInt("sticker_stroke_width_$appWidgetId", 0)
        val initStrokeColor = prefs.getInt("sticker_stroke_color_$appWidgetId", Color.WHITE)

        setContent {
            UCopyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StickerConfigScreen(
                        initialText = initText,
                        initialTheme = initTheme,
                        initialTextColor = initTextColor,
                        initialFontSize = initFontSize,
                        initialFontFamily = initFontFamily,
                        initialShadow = initShadow,
                        initialStrokeWidth = initStrokeWidth,
                        initialStrokeColor = initStrokeColor,
                        onSave = { text, theme, textColor, fontSize, fontFamily, shadow, strokeW, strokeC ->
                            prefs.putString("sticker_text_$appWidgetId", text)
                            prefs.putString("sticker_theme_$appWidgetId", theme)
                            prefs.putInt("sticker_text_color_$appWidgetId", textColor)
                            prefs.putString("sticker_font_size_$appWidgetId", fontSize)
                            prefs.putString("sticker_font_family_$appWidgetId", fontFamily)
                            prefs.putInt("sticker_shadow_radius_$appWidgetId", shadow)
                            prefs.putInt("sticker_stroke_width_$appWidgetId", strokeW)
                            prefs.putInt("sticker_stroke_color_$appWidgetId", strokeC)

                            val manager = AppWidgetManager.getInstance(this)
                            StickerWidgetProvider.updateWidget(this, manager, appWidgetId)

                            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StickerConfigScreen(
    initialText: String,
    initialTheme: String,
    initialTextColor: Int,
    initialFontSize: String,
    initialFontFamily: String,
    initialShadow: Int,
    initialStrokeWidth: Int,
    initialStrokeColor: Int,
    onSave: (String, String, Int, String, String, Int, Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var theme by remember { mutableStateOf(initialTheme) }
    var textColor by remember { mutableStateOf(initialTextColor) }
    var fontSize by remember { mutableStateOf(initialFontSize) }
    var fontFamily by remember { mutableStateOf(initialFontFamily) }
    var shadowRadius by remember { mutableStateOf(initialShadow) }
    var strokeWidth by remember { mutableStateOf(initialStrokeWidth) }
    var strokeColor by remember { mutableStateOf(initialStrokeColor) }

    val colors = listOf(
        androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color(0xFFF9FAFB), androidx.compose.ui.graphics.Color(0xFFF3F4F6),
        androidx.compose.ui.graphics.Color(0xFFE5E7EB), androidx.compose.ui.graphics.Color(0xFFD1D5DB), androidx.compose.ui.graphics.Color(0xFF9CA3AF),
        androidx.compose.ui.graphics.Color(0xFF6B7280), androidx.compose.ui.graphics.Color(0xFF4B5563), androidx.compose.ui.graphics.Color(0xFF374151),
        androidx.compose.ui.graphics.Color(0xFF1F2937), androidx.compose.ui.graphics.Color(0xFF111827), androidx.compose.ui.graphics.Color.Black,
        androidx.compose.ui.graphics.Color(0xFFF87171), androidx.compose.ui.graphics.Color(0xFFEF4444), androidx.compose.ui.graphics.Color(0xFFDC2626),
        androidx.compose.ui.graphics.Color(0xFFFB923C), androidx.compose.ui.graphics.Color(0xFFF97316), androidx.compose.ui.graphics.Color(0xFFEA580C),
        androidx.compose.ui.graphics.Color(0xFFFBBF24), androidx.compose.ui.graphics.Color(0xFFF59E0B), androidx.compose.ui.graphics.Color(0xFFD97706),
        androidx.compose.ui.graphics.Color(0xFFFACC15), androidx.compose.ui.graphics.Color(0xFFEAB308), androidx.compose.ui.graphics.Color(0xFFCA8A04),
        androidx.compose.ui.graphics.Color(0xFFA3E635), androidx.compose.ui.graphics.Color(0xFF84CC16), androidx.compose.ui.graphics.Color(0xFF65A30D),
        androidx.compose.ui.graphics.Color(0xFF4ADE80), androidx.compose.ui.graphics.Color(0xFF22C55E), androidx.compose.ui.graphics.Color(0xFF16A34A),
        androidx.compose.ui.graphics.Color(0xFF34D399), androidx.compose.ui.graphics.Color(0xFF10B981), androidx.compose.ui.graphics.Color(0xFF059669),
        androidx.compose.ui.graphics.Color(0xFF2DD4BF), androidx.compose.ui.graphics.Color(0xFF14B8A6), androidx.compose.ui.graphics.Color(0xFF0D9488),
        androidx.compose.ui.graphics.Color(0xFF38BDF8), androidx.compose.ui.graphics.Color(0xFF0EA5E9), androidx.compose.ui.graphics.Color(0xFF0284C7),
        androidx.compose.ui.graphics.Color(0xFF60A5FA), androidx.compose.ui.graphics.Color(0xFF3B82F6), androidx.compose.ui.graphics.Color(0xFF2563EB),
        androidx.compose.ui.graphics.Color(0xFF818CF8), androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF4F46E5),
        androidx.compose.ui.graphics.Color(0xFFA78BFA), androidx.compose.ui.graphics.Color(0xFF8B5CF6), androidx.compose.ui.graphics.Color(0xFF7C3AED),
        androidx.compose.ui.graphics.Color(0xFFE879F9), androidx.compose.ui.graphics.Color(0xFFD946EF), androidx.compose.ui.graphics.Color(0xFFC026D3),
        androidx.compose.ui.graphics.Color(0xFFF472B6), androidx.compose.ui.graphics.Color(0xFFEC4899), androidx.compose.ui.graphics.Color(0xFFDB2777),
        androidx.compose.ui.graphics.Color(0xFFFB7185), androidx.compose.ui.graphics.Color(0xFFF43F5E), androidx.compose.ui.graphics.Color(0xFFE11D48)
    )

    val bgColor = when (theme) {
        "DARK" -> androidx.compose.ui.graphics.Color(0xFF1E293B)
        "AMOLED" -> androidx.compose.ui.graphics.Color.Black
        "TRANSPARENT" -> androidx.compose.ui.graphics.Color(0xFF333333)
        else -> androidx.compose.ui.graphics.Color(0xFFF8FAFC) // LIGHT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sticker_design), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { onSave(text, theme, textColor, fontSize, fontFamily, shadowRadius, strokeWidth, strokeColor) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.save_widget), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            // FIXED PREVIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(bgColor)
                        .border(
                            width = 1.dp,
                            color = if (theme == "TRANSPARENT") androidx.compose.ui.graphics.Color.Gray else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val previewText = text.ifEmpty { "U Copy" }
                    val textStyle = TextStyle(
                        color = androidx.compose.ui.graphics.Color(textColor),
                        fontSize = when (fontSize) {
                            "SMALL" -> 18.sp
                            "LARGE" -> 32.sp
                            "EXTRA_LARGE" -> 42.sp
                            else -> 24.sp
                        },
                        fontFamily = when (fontFamily) {
                            "SERIF" -> FontFamily.Serif
                            "MONOSPACE" -> FontFamily.Monospace
                            "CURSIVE" -> FontFamily.Cursive
                            else -> FontFamily.Default
                        },
                        fontWeight = FontWeight.Bold,
                        shadow = if (shadowRadius > 0) Shadow(
                            color = androidx.compose.ui.graphics.Color.Black,
                            offset = Offset(0f, shadowRadius.toFloat() / 2),
                            blurRadius = shadowRadius.toFloat()
                        ) else null
                    )

                    // Draw Stroke + Fill
                    if (strokeWidth > 0) {
                        Text(
                            text = previewText,
                            style = textStyle.copy(
                                color = androidx.compose.ui.graphics.Color(strokeColor),
                                drawStyle = Stroke(width = strokeWidth.toFloat() * 2f)
                            ),
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Text(
                        text = previewText,
                        style = textStyle,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            HorizontalDivider()

            // SCROLLABLE SETTINGS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.sticker_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(stringResource(R.string.background_theme), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LIGHT", "DARK", "AMOLED", "TRANSPARENT").forEach { t ->
                        FilterChip(
                            selected = theme == t,
                            onClick = { theme = t },
                            label = { Text(stickerThemeLabel(t), fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Text(stringResource(R.string.text_color), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(colors, textColor) { textColor = it.toArgb() }

                Text(stringResource(R.string.stroke_color), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(colors, strokeColor) { strokeColor = it.toArgb() }

                Text(stringResource(R.string.stroke_width_format, strokeWidth), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Slider(value = strokeWidth.toFloat(), onValueChange = { strokeWidth = it.toInt() }, valueRange = 0f..10f)

                Text(stringResource(R.string.shadow_radius_format, shadowRadius), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Slider(value = shadowRadius.toFloat(), onValueChange = { shadowRadius = it.toInt() }, valueRange = 0f..15f)

                Text(stringResource(R.string.font_size), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE").forEach { size ->
                        FilterChip(
                            selected = fontSize == size,
                            onClick = { fontSize = size },
                            label = { Text(stickerSizeLabel(size), fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Text(stringResource(R.string.font_style), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DEFAULT", "SERIF", "MONOSPACE", "CURSIVE").forEach { fam ->
                        FilterChip(
                            selected = fontFamily == fam,
                            onClick = { fontFamily = fam },
                            label = { Text(stickerFontLabel(fam), fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerRow(colors: List<androidx.compose.ui.graphics.Color>, selected: Int, onSelect: (androidx.compose.ui.graphics.Color) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected == color.toArgb()) 3.dp else 1.dp,
                        color = if (selected == color.toArgb()) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}


@androidx.compose.runtime.Composable
private fun stickerThemeLabel(key: String): String = when (key) {
    "LIGHT" -> stringResource(R.string.sticker_theme_light)
    "DARK" -> stringResource(R.string.sticker_theme_dark)
    "AMOLED" -> stringResource(R.string.sticker_theme_amoled)
    "TRANSPARENT" -> stringResource(R.string.sticker_theme_transparent)
    else -> key
}

@androidx.compose.runtime.Composable
private fun stickerSizeLabel(key: String): String = when (key) {
    "SMALL" -> stringResource(R.string.sticker_size_small)
    "MEDIUM" -> stringResource(R.string.sticker_size_medium)
    "LARGE" -> stringResource(R.string.sticker_size_large)
    "EXTRA_LARGE" -> stringResource(R.string.sticker_size_extra_large)
    else -> key
}

@androidx.compose.runtime.Composable
private fun stickerFontLabel(key: String): String = when (key) {
    "DEFAULT" -> stringResource(R.string.sticker_font_default)
    "SERIF" -> stringResource(R.string.sticker_font_serif)
    "MONOSPACE" -> stringResource(R.string.sticker_font_monospace)
    "CURSIVE" -> stringResource(R.string.sticker_font_cursive)
    else -> key
}
