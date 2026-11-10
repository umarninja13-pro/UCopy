package com.umkt.ucopy.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.umkt.ucopy.R
import com.umkt.ucopy.ui.design.LocalDesignConfig

@Composable
internal fun LicensesTabContent() {
    val context = LocalContext.current
    val jsonString = remember {
        try {
            context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }
    
    val librariesState = produceLibraries(jsonString)
    val libraries = librariesState.value

    // Force all surface colors to match the dialog background to prevent different color boxes
    val baseColors = MaterialTheme.colorScheme
    val customColors = baseColors.copy(
        surface = baseColors.surface,
        background = baseColors.surface,
        surfaceVariant = baseColors.surface,
        surfaceTint = baseColors.surface,
        surfaceContainer = baseColors.surface,
        surfaceContainerLow = baseColors.surface,
        surfaceContainerLowest = baseColors.surface,
        surfaceContainerHigh = baseColors.surface,
        surfaceContainerHighest = baseColors.surface
    )
    
    MaterialTheme(colorScheme = customColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (libraries != null) {
                LibrariesContainer(
                    libraries = libraries,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
