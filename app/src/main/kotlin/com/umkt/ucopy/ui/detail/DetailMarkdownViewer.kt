package com.umkt.ucopy.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.umkt.ucopy.ui.markdown.MarkdownText

@Composable
fun DetailMarkdownViewer(content: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            MarkdownText(
                markdown = content,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
                isSelectable = true,
                autoHighlightLinks = true,
                makeLinksClickable = true,
                onTextClick = onClick,
                parseSynchronously = true
            )
        }
    }
}