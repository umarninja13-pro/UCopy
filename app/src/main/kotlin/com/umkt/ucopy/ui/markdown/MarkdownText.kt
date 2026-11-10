package com.umkt.ucopy.ui.markdown

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.umkt.ucopy.utils.TextAnalyzer
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    highlightQuery: String = "",
    isSelectable: Boolean = false,
    autoHighlightLinks: Boolean = false,
    makeLinksClickable: Boolean = false,
    onTextClick: (() -> Unit)? = null,
    parseSynchronously: Boolean = false
) {
    val context = LocalContext.current
    val textColorArgb = if (color != Color.Unspecified) color.toArgb() else MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val highlightBgColorArgb = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f).toArgb()

    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }

    // If parseSynchronously is true, we parse immediately in remember to prevent raw text flash.
    // Otherwise, we parse in background using produceState for list scrolling performance.
    val finalSpanned = if (parseSynchronously) {
        remember(markdown, highlightQuery, autoHighlightLinks, makeLinksClickable, textColorArgb) {
            try {
                buildFinalSpanned(
                    markdown = markdown,
                    markwon = markwon,
                    highlightQuery = highlightQuery,
                    autoHighlightLinks = autoHighlightLinks,
                    makeLinksClickable = makeLinksClickable,
                    linkColorArgb = linkColorArgb,
                    highlightBgColorArgb = highlightBgColorArgb,
                    context = context
                )
            } catch (e: Exception) {
                SpannableStringBuilder(markdown)
            }
        }
    } else {
        val spannedState = produceState<Spanned?>(initialValue = null, markdown, highlightQuery, autoHighlightLinks, makeLinksClickable, textColorArgb) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    buildFinalSpanned(
                        markdown = markdown,
                        markwon = markwon,
                        highlightQuery = highlightQuery,
                        autoHighlightLinks = autoHighlightLinks,
                        makeLinksClickable = makeLinksClickable,
                        linkColorArgb = linkColorArgb,
                        highlightBgColorArgb = highlightBgColorArgb,
                        context = context
                    )
                } catch (e: Exception) {
                    SpannableStringBuilder(markdown)
                }
            }
        }
        spannedState.value
    }

    Box(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        AndroidView(
            factory = { ctx ->
                android.widget.TextView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setTextIsSelectable(isSelectable)
                    if (makeLinksClickable) {
                        movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    } else {
                        isClickable = false
                        isFocusable = false
                    }
                    
                    if (onTextClick != null) {
                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                        var isLongPress = false
                        var wasSelectionActive = false
                        val longPressRunnable = Runnable { isLongPress = true }

                        setOnTouchListener { v, event ->
                            val textView = v as android.widget.TextView
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    wasSelectionActive = textView.hasSelection()
                                    isLongPress = false
                                    handler.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                                }
                                android.view.MotionEvent.ACTION_UP -> {
                                    handler.removeCallbacks(longPressRunnable)
                                    
                                    if (wasSelectionActive) {
                                        return@setOnTouchListener false
                                    }
                                    
                                    if (!isLongPress) {
                                        val layout = textView.layout
                                        var isLinkClicked = false
                                        if (layout != null) {
                                            val x = event.x.toInt()
                                            val y = event.y.toInt()
                                            val line = layout.getLineForVertical(y)
                                            val off = layout.getOffsetForHorizontal(line, x.toFloat())
                                            val spannable = textView.text as? android.text.Spannable
                                            if (spannable != null) {
                                                val links = spannable.getSpans(off, off, android.text.style.ClickableSpan::class.java)
                                                if (links.isNotEmpty()) {
                                                    isLinkClicked = true
                                                }
                                            }
                                        }
                                        if (!isLinkClicked) {
                                            onTextClick()
                                        }
                                    }
                                }
                                android.view.MotionEvent.ACTION_CANCEL -> {
                                    handler.removeCallbacks(longPressRunnable)
                                }
                            }
                            false
                        }
                    }
                    
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    setLineSpacing(0f, 1.5f)
                    setTextColor(textColorArgb)
                    textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG
                    gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                }
            },
            update = { textView ->
                if (finalSpanned != null) {
                    if (textView.text != finalSpanned) {
                        textView.text = finalSpanned
                        textView.maxLines = maxLines
                        textView.ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                } else {
                    // Fallback to raw text while parsing to prevent jumping
                    val plain = SpannableStringBuilder(markdown)
                    if (textView.text != plain) {
                        textView.text = plain
                        textView.maxLines = maxLines
                        textView.ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                }
                textView.setTextColor(textColorArgb)
                textView.requestLayout()
                textView.invalidate()
            },
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        )
    }
}

private fun buildFinalSpanned(
    markdown: String,
    markwon: Markwon,
    highlightQuery: String,
    autoHighlightLinks: Boolean,
    makeLinksClickable: Boolean,
    linkColorArgb: Int,
    highlightBgColorArgb: Int,
    context: android.content.Context
): Spanned {
    val baseSpanned = markwon.toMarkdown(markdown)
    val builder = SpannableStringBuilder(baseSpanned)

    if (highlightQuery.isNotBlank()) {
        val text = builder.toString()
        val query = highlightQuery.lowercase()
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.lowercase().indexOf(query, startIndex)
            if (index == -1) break
            val end = index + query.length
            builder.setSpan(
                BackgroundColorSpan(highlightBgColorArgb),
                index,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            startIndex = end
        }
    }

    if (autoHighlightLinks && makeLinksClickable) {
        val plainText = builder.toString()
        val detections = TextAnalyzer.analyze(plainText)
        detections.forEach { (type, value) ->
            val start = plainText.indexOf(value)
            if (start >= 0) {
                val end = start + value.length
                builder.setSpan(
                    ForegroundColorSpan(linkColorArgb),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            TextAnalyzer.openAction(context, type, value)
                        }
                    },
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    return builder
}
