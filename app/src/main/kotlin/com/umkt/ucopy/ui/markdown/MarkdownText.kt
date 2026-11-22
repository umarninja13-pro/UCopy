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
import androidx.compose.ui.draw.clipToBounds
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

    val fallbackText = remember(markdown) { SpannableStringBuilder(markdown) }

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
                val target: CharSequence = finalSpanned ?: fallbackText
                if (textView.tag !== target) {
                    textView.tag = target
                    textView.text = target
                    textView.ellipsize = android.text.TextUtils.TruncateAt.END
                    textView.requestLayout()
                }
                if (textView.maxLines != maxLines) textView.maxLines = maxLines
                if (textView.currentTextColor != textColorArgb) textView.setTextColor(textColorArgb)
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
        val lowerText = text.lowercase()
        val query = highlightQuery.lowercase()
        var startIndex = 0
        while (startIndex < lowerText.length) {
            val index = lowerText.indexOf(query, startIndex)
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


private class SelectionAutoScrollTextView(context: android.content.Context) : android.widget.TextView(context) {
    private var ready = false
    private var prevStart = -1
    private var prevEnd = -1
    private var activeOffset = -1
    private var burstUntil = 0L
    private var running = false
    private var fingerDown = false
    private var gestureSelecting = false
    private var ownChange = false
    private var fingerX = 0f
    private var fingerViewportY = 0f
    private var handleTopY = 0f
    private var handleBottomY = 0f
    private val density = context.resources.displayMetrics.density

    private val tick = object : Runnable {
        override fun run() {
            if (step()) postOnAnimation(this) else running = false
        }
    }

    init {
        ready = true
    }

    private fun startTicker() {
        if (!running) {
            running = true
            postOnAnimation(tick)
        }
    }

    private fun updateFinger(event: android.view.MotionEvent) {
        val sv = parent as? android.widget.ScrollView
        fingerX = event.x
        fingerViewportY = event.y - (sv?.scrollY ?: 0)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                fingerDown = true
                gestureSelecting = false
                updateFinger(event)
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                updateFinger(event)
                if (gestureSelecting && fingerDown) startTicker()
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                fingerDown = false
                gestureSelecting = false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!ready) return
        if (!ownChange) {
            if (prevStart == prevEnd && selStart != selEnd) {
                parent?.requestDisallowInterceptTouchEvent(true)
                if (fingerDown) gestureSelecting = true
            }
            if (!gestureSelecting && prevStart != prevEnd && selStart != selEnd) {
                val moved = when {
                    selEnd != prevEnd && selStart == prevStart -> selEnd
                    selStart != prevStart && selEnd == prevEnd -> selStart
                    else -> -1
                }
                val sv = parent as? android.widget.ScrollView
                val lay = layout
                if (moved >= 0 && sv != null && lay != null) {
                    val line = lay.getLineForOffset(moved)
                    handleTopY = (lay.getLineTop(line) + totalPaddingTop - sv.scrollY).toFloat()
                    handleBottomY = (lay.getLineBottom(line) + totalPaddingTop - sv.scrollY).toFloat()
                    activeOffset = moved
                    burstUntil = android.os.SystemClock.uptimeMillis() + 500
                    startTicker()
                }
            }
        }
        prevStart = selStart
        prevEnd = selEnd
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        running = false
        super.onDetachedFromWindow()
    }

    private fun step(): Boolean {
        val sv = parent as? android.widget.ScrollView ?: return false
        val lay = layout ?: return false
        val sp = text as? android.text.Spannable ?: return false
        val fingerMode = gestureSelecting && fingerDown
        val topY: Float
        val bottomY: Float
        if (fingerMode) {
            topY = fingerViewportY
            bottomY = fingerViewportY
        } else {
            if (activeOffset < 0 || activeOffset > sp.length) return false
            if (android.os.SystemClock.uptimeMillis() > burstUntil) return false
            topY = handleTopY
            bottomY = handleBottomY
        }
        val navInset = androidx.core.view.ViewCompat.getRootWindowInsets(this)
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val edge = (96 * density).toInt()
        val bottomZone = sv.height - navInset - edge
        val dy = when {
            bottomY > bottomZone ->
                ((2f + 8f * ((bottomY - bottomZone) / edge).coerceIn(0f, 1.5f)) * density).toInt()
            topY < edge ->
                -((2f + 8f * ((edge - topY) / edge).coerceIn(0f, 1.5f)) * density).toInt()
            else -> 0
        }
        if (dy == 0) return false
        val before = sv.scrollY
        sv.scrollBy(0, dy)
        if (sv.scrollY == before) return false
        if (fingerMode) {
            val localY = (fingerViewportY + sv.scrollY - totalPaddingTop).toInt().coerceIn(0, lay.height)
            val fingerLine = lay.getLineForVertical(localY)
            val off = lay.getOffsetForHorizontal(fingerLine, fingerX - totalPaddingLeft)
            val s = minOf(selectionStart, selectionEnd)
            val e = maxOf(selectionStart, selectionEnd)
            val anchor = if (kotlin.math.abs(s - off) > kotlin.math.abs(e - off)) s else e
            val other = if (anchor == s) e else s
            if (off != other) {
                ownChange = true
                android.text.Selection.setSelection(sp, anchor, off)
                ownChange = false
            }
        }
        return true
    }
}

private const val MENU_SELECT_PARAGRAPH = 0x7001

private object SelectableTextMovement : android.text.method.BaseMovementMethod() {
    override fun canSelectArbitrarily(): Boolean = true
}

@Composable
fun SelectableMarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val textColorArgb = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val highlightBgColorArgb = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f).toArgb()
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }
    val currentOnTap by rememberUpdatedState(onTap)
    val selectParagraphLabel = context.getString(com.umkt.ucopy.R.string.select_paragraph)

    fun build(): Spanned = try {
        buildFinalSpanned(markdown, markwon, "", true, true, linkColorArgb, highlightBgColorArgb, context)
    } catch (e: Exception) {
        SpannableStringBuilder(markdown)
    }

    val spanned: Spanned? = if (markdown.length < 8000) {
        remember(markdown, linkColorArgb) { build() }
    } else {
        produceState<Spanned?>(null, markdown, linkColorArgb) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { build() }
        }.value
    }

    val highlightColorArgb = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f).toArgb()

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { ctx -> ReaderSelectionLayout(ctx) },
        update = { layout ->
            layout.onTap = { currentOnTap() }
            layout.highlightColor = highlightColorArgb
            layout.handleColor = linkColorArgb
            layout.setTextColor(textColorArgb)
            if (spanned != null && layout.contentTag !== spanned) layout.setContent(spanned)
        }
    )
}
