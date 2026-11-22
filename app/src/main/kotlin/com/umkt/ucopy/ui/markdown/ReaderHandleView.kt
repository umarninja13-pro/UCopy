package com.umkt.ucopy.ui.markdown

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

internal class ReaderHandleView(
    context: Context,
    private val onTouch: (MotionEvent) -> Unit
) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val radius = 10f * density
        val centerX = width / 2f
        val centerY = 24f * density
        path.reset()
        path.moveTo(centerX, 2f * density)
        path.lineTo(centerX - radius * 0.7f, centerY - radius * 0.6f)
        path.lineTo(centerX + radius * 0.7f, centerY - radius * 0.6f)
        path.close()
        canvas.drawPath(path, paint)
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        onTouch(event)
        return true
    }
}
