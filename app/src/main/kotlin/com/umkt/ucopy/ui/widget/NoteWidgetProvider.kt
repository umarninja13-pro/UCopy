package com.umkt.ucopy.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import com.umkt.ucopy.MainActivity
import com.umkt.ucopy.R
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View

class NoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return

        when (intent.action) {
            "ACTION_TOGGLE_TODO" -> {
                val isHeaderClick = intent.getBooleanExtra("isHeaderClick", false)
                if (isHeaderClick) {
                    val prefs = UserPreferences(context)
                    val currentState = prefs.getBoolean("widget_show_checked_$appWidgetId", false)
                    prefs.putBoolean("widget_show_checked_$appWidgetId", !currentState)
                    
                    val manager = AppWidgetManager.getInstance(context)
                    manager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), R.id.widget_list)
                    return
                }

                val noteId = intent.getStringExtra("noteId") ?: return
                val todoId = intent.getStringExtra("todoId") ?: return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = ClipRepository(context)
                        val notes = repo.getAll()
                        val note = notes.find { it.id == noteId }
                        if (note != null) {
                            val updatedTodos = note.todoItems.map { 
                                if (it.id == todoId) it.copy(isCompleted = !it.isCompleted) else it 
                            }
                            repo.update(note.copy(todoItems = updatedTodos))
                            
                            val manager = AppWidgetManager.getInstance(context)
                            updateWidget(context, manager, appWidgetId)
                            manager.notifyAppWidgetViewDataChanged(intArrayOf(appWidgetId), R.id.widget_list)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "ACTION_COPY" -> {
                val noteId = intent.getStringExtra("noteId") ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    val repo = ClipRepository(context)
                    val note = repo.getAll().find { it.id == noteId }
                    if (note != null) {
                        withContext(Dispatchers.Main) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), note.content))
                            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @Suppress("DEPRECATION")
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = UserPreferences(context)
                val noteId = prefs.getStringOrNullAsync("widget_note_$appWidgetId") ?: return@launch
                val theme = prefs.getStringAsync("widget_theme_$appWidgetId", "DARK")
                val font = prefs.getStringAsync("widget_font_$appWidgetId", "MEDIUM")
                val fontFamily = prefs.getStringAsync("widget_font_family_$appWidgetId", "DEFAULT")
                val showCopy = prefs.getBooleanAsync("widget_show_copy_$appWidgetId", true)
                val showAdd = prefs.getBooleanAsync("widget_show_add_$appWidgetId", true)
                val showExpand = prefs.getBooleanAsync("widget_show_expand_$appWidgetId", true)
                val showLabel = prefs.getBooleanAsync("widget_show_label_$appWidgetId", true)
                val customTitleColor = prefs.getInt("widget_title_color_$appWidgetId", 0)
                val customBodyColor = prefs.getInt("widget_body_color_$appWidgetId", 0)

                val repository = ClipRepository(context)
                val note = repository.getById(noteId)
                
                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_note)
                    if (note != null) {
                        setupTheme(context, views, theme, customTitleColor)
                        setupFontSize(context, views, font)

                        val titleText = note.title.ifEmpty { context.getString(R.string.untitled) }
                        val titleSpannable = SpannableString(titleText).apply {
                            setSpan(TypefaceSpan(fontFamilyString(fontFamily)), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(StyleSpan(Typeface.BOLD), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        views.setTextViewText(R.id.widget_title, titleSpannable)

                        views.setViewVisibility(R.id.widget_btn_copy, if (showCopy) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.widget_btn_add, if (showAdd) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.widget_btn_expand, if (showExpand) View.VISIBLE else View.GONE)
                        views.setViewVisibility(R.id.widget_date, if (showLabel) View.VISIBLE else View.GONE)

                        // Setup Copy Button
                        val copyIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                            action = "ACTION_COPY"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            putExtra("noteId", note.id)
                        }
                        val copyPending = PendingIntent.getBroadcast(context, appWidgetId + 100, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.widget_btn_copy, copyPending)

                        // Setup Quick Add Button
                        val blankNote = Clip(id = "", title = "", content = "", timestamp = System.currentTimeMillis())
                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("open_clip", json.encodeToString(blankNote))
                        }
                        val addPending = PendingIntent.getActivity(context, appWidgetId + 200, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.widget_btn_add, addPending)

                        // Setup Open App Click
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("open_clip", json.encodeToString(note))
                        }
                        val openPending = PendingIntent.getActivity(context, appWidgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.widget_title, openPending)
                        views.setOnClickPendingIntent(R.id.widget_btn_expand, openPending)

                        // Setup ListView Adapter
                        val listIntent = Intent(context, WidgetTodoService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            putExtra("noteId", note.id)
                            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                        }
                        views.setRemoteAdapter(appWidgetId, R.id.widget_list, listIntent)

                        // Setup List Item Click
                        val toggleIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                            action = "ACTION_TOGGLE_TODO"
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        val togglePending = PendingIntent.getBroadcast(context, appWidgetId + 300, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                        views.setPendingIntentTemplate(R.id.widget_list, togglePending)

                        if (note.isTodo) {
                            views.setViewVisibility(R.id.widget_add_todo_row, android.view.View.VISIBLE)
                            
                            val addTodoIntent = Intent(context, WidgetAddTodoActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("noteId", note.id)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            val addTodoPending = PendingIntent.getActivity(context, appWidgetId + 400, addTodoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            views.setOnClickPendingIntent(R.id.widget_add_todo_row, addTodoPending)
                        } else {
                            views.setViewVisibility(R.id.widget_add_todo_row, android.view.View.GONE)
                        }

                    } else {
                        views.setTextViewText(R.id.widget_title, context.getString(R.string.note_deleted))
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun setupTheme(context: Context, views: RemoteViews, theme: String, customTitleColor: Int) {
            var bgResId = R.drawable.widget_bg_dark
            var titleColor = "#F8FAFC"
            var contentColor = "#CBD5E1"
            var dateColor = "#A5B4FC"

            when (theme) {
                "LIGHT" -> { bgResId = R.drawable.widget_bg_light; titleColor = "#0F172A"; contentColor = "#475569"; dateColor = "#4F46E5" }
                "AMOLED" -> { bgResId = R.drawable.widget_bg_amoled; titleColor = "#FFFFFF"; contentColor = "#D4D4D8"; dateColor = "#A5B4FC" }
                "TRANSPARENT" -> { bgResId = R.drawable.widget_bg_transparent; titleColor = "#FFFFFF"; contentColor = "#E4E4E7"; dateColor = "#A5B4FC" }
            }

            val titleColorInt = android.graphics.Color.parseColor(titleColor)
            val contentColorInt = android.graphics.Color.parseColor(contentColor)
            val dateColorInt = android.graphics.Color.parseColor(dateColor)
            val finalIconColor = if (customTitleColor != 0) customTitleColor else titleColorInt

            views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)
            views.setTextColor(R.id.widget_title, finalIconColor)
            views.setTextColor(R.id.widget_date, dateColorInt)
            
            views.setInt(R.id.widget_btn_copy, "setColorFilter", finalIconColor)
            views.setInt(R.id.widget_btn_add, "setColorFilter", finalIconColor)
            views.setInt(R.id.widget_add_todo_icon, "setColorFilter", finalIconColor)
            views.setInt(R.id.widget_btn_expand, "setColorFilter", finalIconColor)
        }

        private fun setupFontSize(context: Context, views: RemoteViews, size: String) {
            val sp = when (size) {
                "SMALL" -> 15f
                "LARGE" -> 24f
                "EXTRA_LARGE" -> 30f
                else -> 19f
            }
            views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, sp)
        }

        private fun fontFamilyString(family: String): String = when (family) {
            "SERIF" -> "serif"
            "MONOSPACE" -> "monospace"
            "CURSIVE" -> "cursive"
            else -> "sans-serif"
        }

    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = UserPreferences(context)
        for (appWidgetId in appWidgetIds) {
            prefs.remove("widget_note_$appWidgetId")
            prefs.remove("widget_theme_$appWidgetId")
            prefs.remove("widget_font_$appWidgetId")
            prefs.remove("widget_font_family_$appWidgetId")
            prefs.remove("widget_show_copy_$appWidgetId")
            prefs.remove("widget_show_add_$appWidgetId")
            prefs.remove("widget_show_expand_$appWidgetId")
            prefs.remove("widget_show_label_$appWidgetId")
            prefs.remove("widget_show_checked_$appWidgetId")
        }
    }
}
