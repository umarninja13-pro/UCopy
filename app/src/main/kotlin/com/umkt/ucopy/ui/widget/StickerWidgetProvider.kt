package com.umkt.ucopy.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.StaticLayout
import android.text.TextUtils
import android.widget.RemoteViews
import com.umkt.ucopy.R
import com.umkt.ucopy.data.UserPreferences

class StickerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = UserPreferences(context)
        for (appWidgetId in appWidgetIds) {
            prefs.remove("sticker_text_$appWidgetId")
            prefs.remove("sticker_theme_$appWidgetId")
            prefs.remove("sticker_text_color_$appWidgetId")
            prefs.remove("sticker_font_size_$appWidgetId")
            prefs.remove("sticker_font_family_$appWidgetId")
            prefs.remove("sticker_shadow_radius_$appWidgetId")
            prefs.remove("sticker_stroke_width_$appWidgetId")
            prefs.remove("sticker_stroke_color_$appWidgetId")
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = UserPreferences(context)
            
            val text = prefs.getString("sticker_text_$appWidgetId", "U Copy")
            val theme = prefs.getString("sticker_theme_$appWidgetId", "LIGHT")
            val textColor = prefs.getInt("sticker_text_color_$appWidgetId", Color.parseColor("#4F46E5"))
            val fontSize = prefs.getString("sticker_font_size_$appWidgetId", "MEDIUM")
            val fontFamily = prefs.getString("sticker_font_family_$appWidgetId", "DEFAULT")
            val shadowRadius = prefs.getInt("sticker_shadow_radius_$appWidgetId", 4)
            val strokeWidth = prefs.getInt("sticker_stroke_width_$appWidgetId", 0)
            val strokeColor = prefs.getInt("sticker_stroke_color_$appWidgetId", Color.WHITE)

            val views = RemoteViews(context.packageName, R.layout.widget_sticker)

            val bgColor = when (theme) {
                "DARK" -> Color.parseColor("#1E293B")
                "AMOLED" -> Color.BLACK
                "TRANSPARENT" -> Color.TRANSPARENT
                else -> Color.parseColor("#F8FAFC") // LIGHT
            }
            views.setInt(R.id.sticker_root, "setBackgroundColor", bgColor)

            val bitmap = createTextBitmap(context, text, textColor, fontSize, fontFamily, shadowRadius, strokeWidth, strokeColor)
            views.setImageViewBitmap(R.id.sticker_image, bitmap)

            val configIntent = Intent(context, StickerConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.sticker_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createTextBitmap(
            context: Context, 
            text: String, 
            color: Int, 
            sizeStr: String, 
            familyStr: String, 
            shadowRadius: Int,
            strokeWidth: Int,
            strokeColor: Int
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val width = 1000
            val height = 1000
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val sp = when (sizeStr) {
                "SMALL" -> 20f
                "LARGE" -> 50f
                "EXTRA_LARGE" -> 70f
                else -> 36f
            }
            val textSizePx = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.resources.displayMetrics
            )

            val typeface = when (familyStr) {
                "SERIF" -> Typeface.SERIF
                "MONOSPACE" -> Typeface.MONOSPACE
                "CURSIVE" -> Typeface.create("cursive", Typeface.NORMAL)
                else -> Typeface.DEFAULT_BOLD
            }

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.textSize = textSizePx
                this.typeface = typeface
                if (shadowRadius > 0) {
                    setShadowLayer(shadowRadius.toFloat(), 0f, shadowRadius.toFloat() / 2, Color.BLACK)
                }
            }

            val textWidth = width - 100 // 100px padding on sides
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .setMaxLines(4)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()

            val xOffset = 50f
            val yOffset = (height - staticLayout.height) / 2f

            if (strokeWidth > 0) {
                val strokePaint = TextPaint(textPaint).apply {
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth * density
                    this.color = strokeColor
                    if (shadowRadius > 0) {
                        setShadowLayer(shadowRadius.toFloat(), 0f, shadowRadius.toFloat() / 2, Color.BLACK)
                    }
                }
                val strokeLayout = StaticLayout.Builder.obtain(text, 0, text.length, strokePaint, textWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .setMaxLines(4)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()

                canvas.save()
                canvas.translate(xOffset, yOffset)
                strokeLayout.draw(canvas)
                canvas.restore()
            }

            canvas.save()
            canvas.translate(xOffset, yOffset)
            staticLayout.draw(canvas)
            canvas.restore()

            return bitmap
        }
    }
}
