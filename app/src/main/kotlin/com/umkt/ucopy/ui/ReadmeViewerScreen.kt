package com.umkt.ucopy.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.ui.markdown.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadmeViewerScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val readmeText = remember {
        try {
            context.assets.open("README.md").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            context.getString(R.string.readme_not_found)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_documentation), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        MarkdownText(
            markdown = readmeText,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            isSelectable = true,
            makeLinksClickable = true
        )
    }
}
