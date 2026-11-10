package com.umkt.ucopy.ui.card

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class NonTouchWebView(context: Context) : WebView(context) {
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false // Let the Compose parent handle the click
    }
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlCardView(
    htmlContent: String, 
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val scaledHtml = """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body { 
                margin: 0; 
                padding: 4px; 
                transform: scale(0.4); 
                transform-origin: top left; 
                width: 250%; 
                background: transparent;
                pointer-events: none; /* Disable inner touches */
            }
            * { font-size: 14px !important; }
        </style>
        </head>
        <body>$htmlContent</body>
        </html>
    """.trimIndent()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RectangleShape)
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                NonTouchWebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    loadDataWithBaseURL(null, scaledHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
