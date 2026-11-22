package com.umkt.ucopy.ui.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

internal object ReaderTextActions {

    private fun isWordChar(c: Char): Boolean =
        c.isLetterOrDigit() || Character.getType(c) == Character.NON_SPACING_MARK.toInt()

    fun wordBounds(text: CharSequence, offset: Int): Pair<Int, Int> {
        val length = text.length
        var start = offset.coerceIn(0, length)
        var end = start
        while (start > 0 && isWordChar(text[start - 1])) start--
        while (end < length && isWordChar(text[end])) end++
        if (start == end) {
            end = minOf(length, start + 1)
            if (start == end && start > 0) start -= 1
        }
        return Pair(start, end)
    }

    fun paragraphBounds(text: CharSequence, from: Int, to: Int): Pair<Int, Int> {
        var start = from.coerceIn(0, text.length)
        var end = to.coerceIn(0, text.length)
        while (start > 0 && text[start - 1] != '\n') start--
        while (end < text.length && text[end] != '\n') end++
        return Pair(start, end)
    }

    fun copy(context: Context, text: CharSequence) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("U Copy", text))
    }

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
