package com.umkt.ucopy.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.umkt.ucopy.ui.markdown.SelectableMarkdownView

@Composable
fun DetailMarkdownViewer(content: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectableMarkdownView(
        markdown = content,
        onTap = onClick,
        modifier = modifier
    )
}
