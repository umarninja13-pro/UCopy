package com.umkt.ucopy.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.umkt.ucopy.R

enum class DesignMode { DEFAULT, NOTEBOOKS, LIST, TASK }
enum class SearchStyle { TRANSPARENT, PILL, BORDERED, UNDERLINE }
enum class CardStyle { TONAL, PAPER, STICKY, BORDERLESS, GEMINI, LIST, TASK }
enum class FabStyle { SQUIRCLE, CIRCLE, EXTENDED }

data class DesignConfig(
    val mode: DesignMode,
    @StringRes val displayNameRes: Int,
    @StringRes val taglineRes: Int,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val amoledScheme: ColorScheme,
    val cardShape: Dp,
    val fabShape: Dp,
    val fabSize: Dp,
    val searchStyle: SearchStyle,
    val cardStyle: CardStyle,
    val fabStyle: FabStyle,
    val showCardDividers: Boolean,
    val cardAlpha: Float,
    val listSpacing: Dp,
    val useDynamicColor: Boolean
)

val LocalDesignConfig = staticCompositionLocalOf { DesignPresets.get(DesignMode.DEFAULT) }

object DesignPresets {
    fun get(mode: DesignMode): DesignConfig = when (mode) {
        DesignMode.DEFAULT -> defaultDesign
        DesignMode.NOTEBOOKS -> notebooksDesign
        DesignMode.LIST -> listDesign
        DesignMode.TASK -> taskDesign
    }
    fun all(): List<DesignConfig> = DesignMode.values().map { get(it) }
}

// ── DEFAULT (آپ کی موجودہ ایپ) ──────────────────────────────────
private val DefaultLightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE), onSecondaryContainer = Color(0xFF082F49),
    tertiary = Color(0xFF8B5CF6), onTertiary = Color.White,
    background = Color(0xFFF8FAFC), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0), outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFEF4444), onError = Color.White
)
private val DefaultDarkScheme = darkColorScheme(
    primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81), onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8), onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E), onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA78BFA), onTertiary = Color(0xFF2E1065),
    background = Color(0xFF0F172A), onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B), onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155), onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155), outlineVariant = Color(0xFF475569),
    error = Color(0xFFF87171), onError = Color(0xFF450A0A)
)
private val DefaultAmoledScheme = DefaultDarkScheme.copy(
    background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF141414)
)


private val defaultDesign = DesignConfig(
    mode = DesignMode.DEFAULT, displayNameRes = R.string.design_default_name,
    taglineRes = R.string.design_default_tagline,
    lightScheme = DefaultLightScheme, darkScheme = DefaultDarkScheme, amoledScheme = DefaultAmoledScheme,
    cardShape = 24.dp, fabShape = 24.dp, fabSize = 64.dp,
    searchStyle = SearchStyle.TRANSPARENT, cardStyle = CardStyle.PAPER, fabStyle = FabStyle.SQUIRCLE,
    showCardDividers = false, cardAlpha = 1f, listSpacing = 6.dp, useDynamicColor = true
)

// ── NOTEBOOKS (Gemini Style) ─────────────────────────────────────
private val NotebookLight = lightColorScheme(
    primary = Color(0xFF1A73E8), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE), onPrimaryContainer = Color(0xFF0A2E6E),
    secondary = Color(0xFFEA8600), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF7E0), onSecondaryContainer = Color(0xFF5A3D00),
    tertiary = Color(0xFF188038), onTertiary = Color.White,
    background = Color(0xFFFAF9F6), onBackground = Color(0xFF1F1F1F),
    surface = Color.White, onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF1F0EC), onSurfaceVariant = Color(0xFF5F5E5A),
    outline = Color(0xFF8E8E8E), outlineVariant = Color(0xFFE0DFDB),
    error = Color(0xFFBA1A1A), onError = Color.White
)
private val NotebookDark = darkColorScheme(
    primary = Color(0xFF8AB4F8), onPrimary = Color(0xFF0A2E6E),
    primaryContainer = Color(0xFF1F3D6E), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFFDD663), onSecondary = Color(0xFF5A3D00),
    secondaryContainer = Color(0xFF4A3A12), onSecondaryContainer = Color(0xFFFFF0C4),
    tertiary = Color(0xFF81C995), onTertiary = Color(0xFF00391A),
    background = Color(0xFF1F1F1F), onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF2A2A2A), onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF3A3A3A), onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF4A4A4A),
    error = Color(0xFFFFB4AB), onError = Color(0xFF1F1F1F)
)
private val NotebookAmoled = NotebookDark.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF141414))

private val notebooksDesign = DesignConfig(
    mode = DesignMode.NOTEBOOKS, displayNameRes = R.string.design_notebooks_name,
    taglineRes = R.string.design_notebooks_tagline,
    lightScheme = NotebookLight, darkScheme = NotebookDark, amoledScheme = NotebookAmoled,
    cardShape = 20.dp, fabShape = 100.dp, fabSize = 64.dp,
    searchStyle = SearchStyle.PILL, cardStyle = CardStyle.GEMINI, fabStyle = FabStyle.EXTENDED,
    showCardDividers = false, cardAlpha = 1f, listSpacing = 12.dp, useDynamicColor = false
)

// ── LIST (Clean Notes / Apple Notes Style) ───────────────────────
private val ListLight = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF44474F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F3F4), onSecondaryContainer = Color(0xFF1A1C1E),
    tertiary = Color(0xFF6750A4), onTertiary = Color.White,
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF1A1C1E),
    surface = Color.White, onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF8F9FA), onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFFDDE1E6), outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFBA1A1A), onError = Color.White
)
private val ListDark = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF041E49),
    primaryContainer = Color(0xFF1F3A6E), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFC4C6CF), onSecondary = Color(0xFF1A1C1E),
    secondaryContainer = Color(0xFF2A2B2E), onSecondaryContainer = Color(0xFFE2E2E6),
    tertiary = Color(0xFFD0BCFF), onTertiary = Color(0xFF381E72),
    background = Color(0xFF121316), onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF121316), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF1E1F22), onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF3A3B3E), outlineVariant = Color(0xFF2A2B2E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF121316)
)
private val ListAmoled = ListDark.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF141414))

private val listDesign = DesignConfig(
    mode = DesignMode.LIST, displayNameRes = R.string.design_simple_name,
    taglineRes = R.string.design_simple_tagline,
    lightScheme = ListLight, darkScheme = ListDark, amoledScheme = ListAmoled,
    cardShape = 0.dp, fabShape = 16.dp, fabSize = 56.dp,
    searchStyle = SearchStyle.BORDERED, cardStyle = CardStyle.LIST, fabStyle = FabStyle.SQUIRCLE,
    showCardDividers = true, cardAlpha = 0f, listSpacing = 0.dp, useDynamicColor = false
)

// ── TASK (To-Do Style) ───────────────────────────────────────────
private val TaskLight = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFFB3261E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF9DEDC), onSecondaryContainer = Color(0xFF410E0B),
    tertiary = Color(0xFF188038), onTertiary = Color.White,
    background = Color(0xFFF8F9FA), onBackground = Color(0xFF1A1C1E),
    surface = Color.White, onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFFDDE1E6), outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFBA1A1A), onError = Color.White
)
private val TaskDark = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF041E49),
    primaryContainer = Color(0xFF1F3A6E), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFFFB4AB), onSecondary = Color(0xFF410E0B),
    secondaryContainer = Color(0xFF7A2E2E), onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF81C995), onTertiary = Color(0xFF00391A),
    background = Color(0xFF121316), onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E1F22), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF1E1F22), onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF3A3B3E), outlineVariant = Color(0xFF2A2B2E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF121316)
)
private val TaskAmoled = TaskDark.copy(background = Color.Black, surface = Color(0xFF1A1A1A), surfaceVariant = Color(0xFF1A1A1A))

private val taskDesign = DesignConfig(
    mode = DesignMode.TASK, displayNameRes = R.string.design_sticky_name,
    taglineRes = R.string.design_sticky_tagline,
    lightScheme = TaskLight, darkScheme = TaskDark, amoledScheme = TaskAmoled,
    cardShape = 16.dp, fabShape = 100.dp, fabSize = 64.dp,
    searchStyle = SearchStyle.PILL, cardStyle = CardStyle.TASK, fabStyle = FabStyle.EXTENDED,
    showCardDividers = false, cardAlpha = 1f, listSpacing = 10.dp, useDynamicColor = false
)
