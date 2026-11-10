package com.umkt.ucopy.ui.detail

import com.umkt.ucopy.R

class WebAppInterface(private val context: android.content.Context) {
    @android.webkit.JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("U Copy", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, context.getString(R.string.copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show()
    }
}
