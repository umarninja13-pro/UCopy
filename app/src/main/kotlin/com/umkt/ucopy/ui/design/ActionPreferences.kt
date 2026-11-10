package com.umkt.ucopy.ui.design

import androidx.compose.runtime.staticCompositionLocalOf

data class ActionPreferences(
    val showCopy: Boolean = true,
    val showShare: Boolean = true,
    val markdownPreview: Boolean = true,
    val htmlPreview: Boolean = true,
    val showOpeningAnimation: Boolean = true,
    val showListAnimations: Boolean = true
)

val LocalActionPreferences = staticCompositionLocalOf { ActionPreferences() }
