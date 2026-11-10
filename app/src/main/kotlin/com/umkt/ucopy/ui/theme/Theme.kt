package com.umkt.ucopy.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.umkt.ucopy.ui.design.DesignConfig
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.DesignPresets
import com.umkt.ucopy.ui.design.LocalDesignConfig

enum class ThemeMode { LIGHT, DARK, AMOLED, SYSTEM, TIME_BASED }

@Composable
fun UCopyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    designMode: DesignMode = DesignMode.DEFAULT,
    content: @Composable () -> Unit
) {
    val design: DesignConfig = remember(designMode) { DesignPresets.get(designMode) }

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.TIME_BASED -> {
            val context = LocalContext.current
            val config = TimeBasedThemeConfig.load(context)
            if (config.enabled) {
                val currentTime = java.time.LocalTime.now()
                val effective = config.getEffectiveTheme(currentTime)
                when (effective) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.AMOLED -> true
                    else -> isSystemInDarkTheme()
                }
            } else {
                isSystemInDarkTheme()
            }
        }
    }

    val context = LocalContext.current
    val colorScheme = remember(themeMode, dynamicColor, designMode, darkTheme) {
        when {
            dynamicColor && design.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val baseScheme =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (themeMode == ThemeMode.AMOLED) {
                    baseScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceVariant = Color(0xFF141414)
                    )
                } else {
                    baseScheme
                }
            }
            themeMode == ThemeMode.AMOLED -> design.amoledScheme
            darkTheme -> design.darkScheme
            else -> design.lightScheme
        }
    }

    val animatedColorScheme = animateColorSchemeSmooth(colorScheme)

    val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(design.cardShape / 3),
        small = androidx.compose.foundation.shape.RoundedCornerShape(design.cardShape / 2),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(design.cardShape * 3 / 4),
        large = androidx.compose.foundation.shape.RoundedCornerShape(design.cardShape),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(design.cardShape * 5 / 4)
    )

    CompositionLocalProvider(LocalDesignConfig provides design) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = Typography(),
            shapes = shapes,
            content = content
        )
    }
}



@Composable
private fun animateColorSchemeSmooth(target: ColorScheme, durationMs: Int = 320): ColorScheme {
    val spec = tween<Color>(durationMs, easing = FastOutSlowInEasing)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, spec, label = "onTertiary").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(target.outline, spec, label = "outline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "outlineVariant").value,
        error = animateColorAsState(target.error, spec, label = "error").value,
        onError = animateColorAsState(target.onError, spec, label = "onError").value
    )
}
