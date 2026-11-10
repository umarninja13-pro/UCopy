package com.umkt.ucopy.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.umkt.ucopy.R
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.model.Task
import kotlinx.coroutines.runBlocking
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan

class WidgetTodoService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetTodoFactory(applicationContext, intent)
    }
}

class WidgetTodoFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private val noteId = intent.getStringExtra("noteId")
    
    private var note: Clip? = null
    private var uncheckedItems: List<Task> = emptyList()
    private var checkedItems: List<Task> = emptyList()
    private var theme: String = "DARK"
    private var showChecked: Boolean = false

    private val markwon by lazy {
        io.noties.markwon.Markwon.builder(context)
            .usePlugin(io.noties.markwon.html.HtmlPlugin.create())
            .build()
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        if (noteId != null) {
            val repo = ClipRepository(context)
            note = runBlocking { repo.getAll().find { it.id == noteId } }
            note?.let { n ->
                uncheckedItems = n.todoItems.filter { !it.isCompleted }
                checkedItems = n.todoItems.filter { it.isCompleted }
            }
            val prefs = UserPreferences(context)
            theme = runBlocking { prefs.getStringAsync("widget_theme_$appWidgetId", "DARK") }
            showChecked = runBlocking { prefs.getBooleanAsync("widget_show_checked_$appWidgetId", false) }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        val n = note ?: return 0
        if (!n.isTodo) return 1
        
        var count = uncheckedItems.size
        if (checkedItems.isNotEmpty()) {
            count += 1 // Header
            if (showChecked) {
                count += checkedItems.size
            }
        }
        return count
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val n = note ?: return null
        val prefs = UserPreferences(context)
        
        val theme = prefs.getString("widget_theme_$appWidgetId", "DARK")
        val font = prefs.getString("widget_font_$appWidgetId", "MEDIUM")
        val fontFamily = prefs.getString("widget_font_family_$appWidgetId", "DEFAULT")
        val customBodyColor = prefs.getInt("widget_body_color_$appWidgetId", 0)
        val contentSp = when (font) {
            "SMALL" -> 11f
            "LARGE" -> 18f
            "EXTRA_LARGE" -> 22f
            else -> 14f
        }
        var contentColor = "#CBD5E1"
        when (theme) {
            "LIGHT" -> contentColor = "#475569"
            "AMOLED" -> contentColor = "#D4D4D8"
            "TRANSPARENT" -> contentColor = "#E4E4E7"
        }
        val colorInt = Color.parseColor(contentColor)

        if (!n.isTodo) {
            val views = RemoteViews(context.packageName, R.layout.widget_text_item)
            val rawContent = n.content
            
            val parsedSpanned = try {
                markwon.toMarkdown(rawContent)
            } catch (e: Exception) {
                SpannableString(rawContent)
            }
            
            val contentSpannable = android.text.SpannableStringBuilder(parsedSpanned).apply {
                setSpan(TypefaceSpan(fontFamilyString(fontFamily)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            
            views.setTextViewText(R.id.text_content, contentSpannable)
            views.setTextColor(R.id.text_content, if (customBodyColor != 0) customBodyColor else colorInt)
            views.setTextViewTextSize(R.id.text_content, android.util.TypedValue.COMPLEX_UNIT_SP, contentSp)
            return views
        }

        // 1. Unchecked Items
        if (position < uncheckedItems.size) {
            return createTodoView(uncheckedItems[position], false, colorInt, contentSp, fontFamily, n.id, customBodyColor)
        }

        // 2. Header
        if (checkedItems.isNotEmpty() && position == uncheckedItems.size) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo_header)
            views.setTextViewText(R.id.todo_header_text, context.getString(R.string.checked_items_format, checkedItems.size))
            views.setTextColor(R.id.todo_header_text, Color.parseColor("#888888"))
            views.setInt(R.id.todo_header_icon, "setColorFilter", Color.parseColor("#888888"))
            
            if (showChecked) {
                views.setImageViewResource(R.id.todo_header_icon, R.drawable.ic_expand_less)
            } else {
                views.setImageViewResource(R.id.todo_header_icon, R.drawable.ic_expand_more)
            }

            val fillInIntent = Intent().apply {
                putExtra("isHeaderClick", true)
                putExtra("noteId", noteId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickFillInIntent(R.id.todo_header_row, fillInIntent)
            return views
        }

        // 3. Checked Items (only if expanded)
        if (showChecked) {
            val checkedIndex = position - uncheckedItems.size - 1
            if (checkedIndex in checkedItems.indices) {
                return createTodoView(checkedItems[checkedIndex], true, colorInt, contentSp, fontFamily, n.id, customBodyColor)
            }
        }

        return null
    }

    private fun createTodoView(todo: Task, isChecked: Boolean, colorInt: Int, fontSp: Float, fontFamily: String, noteId: String, customBodyColor: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_todo_item)
        val todoSpannable = SpannableString(todo.title).apply {
            setSpan(TypefaceSpan(fontFamilyString(fontFamily)), 0, todo.title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        views.setTextViewText(R.id.todo_text, todoSpannable)
        views.setTextViewTextSize(R.id.todo_text, android.util.TypedValue.COMPLEX_UNIT_SP, fontSp)
        views.setImageViewResource(R.id.todo_checkbox, if (isChecked) R.drawable.ic_todo_checked else R.drawable.ic_todo_unchecked)
        
        if (isChecked) {
            views.setTextColor(R.id.todo_text, Color.parseColor("#888888"))
            views.setInt(R.id.todo_text, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        } else {
            views.setTextColor(R.id.todo_text, if (customBodyColor != 0) customBodyColor else colorInt)
            views.setInt(R.id.todo_text, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
        }

        val fillInIntent = Intent().apply {
            putExtra("isHeaderClick", false)
            putExtra("noteId", noteId)
            putExtra("todoId", todo.id)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setOnClickFillInIntent(R.id.todo_row, fillInIntent)
        return views
    }

    private fun fontFamilyString(family: String): String = when (family) {
        "SERIF" -> "serif"
        "MONOSPACE" -> "monospace"
        "CURSIVE" -> "cursive"
        else -> "sans-serif"
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 3
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
