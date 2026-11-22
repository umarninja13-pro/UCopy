package com.umkt.ucopy.ui.markdown

import android.content.Context
import android.graphics.Rect
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.ActionMode
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

internal class ReaderSelectionLayout(context: Context) : FrameLayout(context), ReaderSelectionHost {

    private val density = context.resources.displayMetrics.density
    private val scrollView = ScrollView(context)
    private val textView = TextView(context)
    private val startHandle = ReaderHandleView(context) { event -> onHandleTouch(true, event) }
    private val endHandle = ReaderHandleView(context) { event -> onHandleTouch(false, event) }
    private var highlightSpan: BackgroundColorSpan? = null
    private var selStart = -1
    private var selEnd = -1
    private var anchorStart = -1
    private var anchorEnd = -1
    private var actionMode: ActionMode? = null
    private var finishingMode = false
    private var dragMode = DRAG_NONE
    private var fingerX = 0f
    private var fingerY = 0f
    private var tickerRunning = false

    var onTap: () -> Unit = {}

    var highlightColor: Int = 0x553F51B5
        set(value) {
            field = value
            if (hasSelection()) applyHighlight()
        }

    var handleColor: Int = 0xFF3F51B5.toInt()
        set(value) {
            field = value
            startHandle.setColor(value)
            endHandle.setColor(value)
        }

    val contentTag: Any?
        get() = textView.tag

    private fun dp(value: Int): Int = (value * density).toInt()

    private val tick = object : Runnable {
        override fun run() {
            if (dragMode == DRAG_NONE) {
                tickerRunning = false
                return
            }
            scrollStep()
            postOnAnimation(this)
        }
    }

    private val toolbarCallback = ReaderToolbarCallback(this, context)

    init {
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        textView.setLineSpacing(0f, 1.5f)
        textView.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        textView.setPadding(0, dp(8), 0, dp(120))
        textView.setTextIsSelectable(false)

        val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleTap(e)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                handleLongPress(e)
            }
        })
        textView.setOnTouchListener { _, event ->
            gestures.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> if (dragMode == DRAG_LONGPRESS) {
                    fingerX = event.x
                    fingerY = event.y - scrollView.scrollY
                    applyDrag()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragMode == DRAG_LONGPRESS) endDrag()
            }
            true
        }

        scrollView.isVerticalScrollBarEnabled = true
        scrollView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        scrollView.addView(textView)
        scrollView.setOnScrollChangeListener { _, _, _, _, _ -> onScrolled() }
        addView(scrollView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        startHandle.setColor(handleColor)
        endHandle.setColor(handleColor)
        for (handle in listOf(startHandle, endHandle)) {
            handle.visibility = View.GONE
            addView(handle, FrameLayout.LayoutParams(dp(HANDLE_WIDTH), dp(HANDLE_HEIGHT)))
        }
    }

    fun setContent(spanned: Spanned) {
        clearSelection()
        textView.setText(spanned, TextView.BufferType.SPANNABLE)
        textView.tag = spanned
    }

    fun setTextColor(color: Int) {
        if (textView.currentTextColor != color) textView.setTextColor(color)
    }

    override fun hasSelection(): Boolean = selStart >= 0 && selEnd > selStart

    override fun selectedText(): CharSequence = textView.text.subSequence(selStart, selEnd)

    override fun selectAll() {
        setSelection(0, textView.text.length)
    }

    override fun selectParagraph() {
        val bounds = ReaderTextActions.paragraphBounds(textView.text, selStart, selEnd)
        setSelection(bounds.first, bounds.second)
    }

    override fun onToolbarDestroyed() {
        actionMode = null
        if (!finishingMode) clearSelection()
    }

    private fun handleTap(e: MotionEvent) {
        if (hasSelection()) {
            clearSelection()
            return
        }
        val layout = textView.layout
        val spanned = textView.text as? Spanned
        if (layout != null && spanned != null) {
            val y = (e.y - textView.totalPaddingTop).toInt()
            val x = e.x - textView.totalPaddingLeft
            val line = layout.getLineForVertical(y)
            if (x >= layout.getLineLeft(line) && x <= layout.getLineRight(line)) {
                val offset = layout.getOffsetForHorizontal(line, x)
                val link = spanned.getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()
                if (link != null) {
                    link.onClick(textView)
                    return
                }
            }
        }
        onTap()
    }

    private fun handleLongPress(e: MotionEvent) {
        val length = textView.text.length
        if (length == 0) return
        val offset = textView.getOffsetForPosition(e.x, e.y)
        val bounds = ReaderTextActions.wordBounds(textView.text, offset)
        anchorStart = bounds.first
        anchorEnd = bounds.second
        textView.parent?.requestDisallowInterceptTouchEvent(true)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        dragMode = DRAG_LONGPRESS
        fingerX = e.x
        fingerY = e.y - scrollView.scrollY
        setSelection(anchorStart, anchorEnd)
        startTicker()
    }

    private fun onHandleTouch(isStart: Boolean, e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                dragMode = if (isStart) DRAG_START else DRAG_END
                trackRaw(e)
                hideToolbar()
                startTicker()
            }
            MotionEvent.ACTION_MOVE -> {
                trackRaw(e)
                applyDrag()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endDrag()
        }
    }

    private fun trackRaw(e: MotionEvent) {
        val location = IntArray(2)
        getLocationOnScreen(location)
        fingerX = e.rawX - location[0]
        fingerY = e.rawY - location[1]
    }

    private fun applyDrag() {
        if (!hasSelection()) return
        val localY = fingerY + scrollView.scrollY
        when (dragMode) {
            DRAG_LONGPRESS -> {
                val offset = textView.getOffsetForPosition(fingerX, localY)
                when {
                    offset >= anchorEnd -> setSelection(anchorStart, maxOf(offset, anchorEnd))
                    offset <= anchorStart -> setSelection(minOf(offset, anchorStart), anchorEnd)
                    else -> setSelection(anchorStart, anchorEnd)
                }
            }
            DRAG_START -> {
                val offset = textView.getOffsetForPosition(fingerX, localY - dp(HANDLE_TOUCH_OFFSET))
                if (offset < selEnd) setSelection(offset, selEnd)
            }
            DRAG_END -> {
                val offset = textView.getOffsetForPosition(fingerX, localY - dp(HANDLE_TOUCH_OFFSET))
                if (offset > selStart) setSelection(selStart, offset)
            }
        }
    }

    private fun endDrag() {
        dragMode = DRAG_NONE
        showToolbar()
    }

    private fun setSelection(start: Int, end: Int) {
        selStart = start
        selEnd = end
        applyHighlight()
        positionHandles()
        if (actionMode == null) {
            actionMode = startActionMode(toolbarCallback, ActionMode.TYPE_FLOATING)
        }
        if (dragMode != DRAG_NONE) hideToolbar() else actionMode?.invalidateContentRect()
    }

    private fun applyHighlight() {
        val spannable = textView.text as? Spannable ?: return
        highlightSpan?.let { spannable.removeSpan(it) }
        val span = BackgroundColorSpan(highlightColor)
        spannable.setSpan(span, selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        highlightSpan = span
    }

    override fun clearSelection() {
        val spannable = textView.text as? Spannable
        highlightSpan?.let { spannable?.removeSpan(it) }
        highlightSpan = null
        selStart = -1
        selEnd = -1
        dragMode = DRAG_NONE
        startHandle.visibility = View.GONE
        endHandle.visibility = View.GONE
        finishToolbar()
    }

    private fun finishToolbar() {
        val mode = actionMode ?: return
        finishingMode = true
        actionMode = null
        mode.finish()
        finishingMode = false
    }

    private fun hideToolbar() {
        actionMode?.hide(ActionMode.DEFAULT_HIDE_DURATION.toLong())
    }

    private fun showToolbar() {
        actionMode?.hide(0L)
        actionMode?.invalidateContentRect()
    }

    private fun positionHandles() {
        if (!hasSelection()) return
        placeHandle(startHandle, selStart)
        placeHandle(endHandle, selEnd)
    }

    private fun placeHandle(handle: View, offset: Int) {
        val layout = textView.layout ?: return
        val line = layout.getLineForOffset(offset)
        val x = layout.getPrimaryHorizontal(offset) + textView.totalPaddingLeft
        val y = layout.getLineBottom(line) + textView.totalPaddingTop - scrollView.scrollY
        handle.translationX = x - dp(HANDLE_WIDTH) / 2f
        handle.translationY = y.toFloat()
        val offscreen = y < 0 || y > height + dp(HANDLE_HEIGHT)
        handle.visibility = if (offscreen && dragMode == DRAG_NONE) View.INVISIBLE else View.VISIBLE
    }

    private fun onScrolled() {
        positionHandles()
        actionMode?.invalidateContentRect()
    }

    override fun fillSelectionRect(outRect: Rect) {
        val layout = textView.layout
        if (layout == null || !hasSelection()) {
            outRect.set(0, 0, 1, 1)
            return
        }
        val startLine = layout.getLineForOffset(selStart)
        val endLine = layout.getLineForOffset(selEnd)
        val startX = layout.getPrimaryHorizontal(selStart) + textView.totalPaddingLeft
        val endX = layout.getPrimaryHorizontal(selEnd) + textView.totalPaddingLeft
        val top = (layout.getLineTop(startLine) + textView.totalPaddingTop - scrollView.scrollY).coerceAtLeast(0)
        val bottom = (layout.getLineBottom(endLine) + textView.totalPaddingTop - scrollView.scrollY)
            .coerceAtMost(height).coerceAtLeast(top + 1)
        val left = if (startLine == endLine) minOf(startX, endX).toInt() else 0
        val right = if (startLine == endLine) maxOf(startX, endX).toInt() else width
        outRect.set(left, top, maxOf(right, left + 1), bottom)
    }

    private fun startTicker() {
        if (!tickerRunning) {
            tickerRunning = true
            postOnAnimation(tick)
        }
    }

    private fun scrollStep() {
        val navInset = ViewCompat.getRootWindowInsets(this)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val zone = dp(AUTOSCROLL_ZONE).toFloat()
        val bottomLimit = height - navInset - zone
        val dy = when {
            fingerY > bottomLimit ->
                ((3f + 11f * ((fingerY - bottomLimit) / zone).coerceIn(0f, 1f)) * density).toInt()
            fingerY < zone ->
                -((3f + 11f * ((zone - fingerY) / zone).coerceIn(0f, 1f)) * density).toInt()
            else -> 0
        }
        if (dy == 0) return
        val before = scrollView.scrollY
        scrollView.scrollBy(0, dy)
        if (scrollView.scrollY != before) applyDrag()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        tickerRunning = false
        finishToolbar()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DRAG_NONE = 0
        const val DRAG_LONGPRESS = 1
        const val DRAG_START = 2
        const val DRAG_END = 3
        const val HANDLE_WIDTH = 40
        const val HANDLE_HEIGHT = 48
        const val HANDLE_TOUCH_OFFSET = 28
        const val AUTOSCROLL_ZONE = 100
    }
}
