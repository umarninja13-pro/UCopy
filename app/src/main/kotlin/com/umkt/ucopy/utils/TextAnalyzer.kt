package com.umkt.ucopy.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object TextAnalyzer {

    private val urlPattern = Regex("""(?:https?://|www\.)[^\s<>"']+""")
    private val emailPattern = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
    private val phonePattern = Regex("""\+?\d[\d\s().-]{7,}\d""")
    private val trailingPunct = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

    fun analyze(text: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()

        urlPattern.findAll(text).forEach { match ->
            var url = match.value
            while (url.isNotEmpty() && url.last() in trailingPunct) url = url.dropLast(1)
            if (url.isNotEmpty() && !results.any { it.second == url }) results.add("URL" to url)
        }

        emailPattern.findAll(text).forEach { match ->
            val email = match.value
            if (!results.any { it.second == email }) results.add("Email" to email)
        }

        phonePattern.findAll(text).forEach { match ->
            val phone = match.value.trim()
            if (phone.length >= 7 && !results.any { it.second == phone }) results.add("Phone" to phone)
        }

        return results
    }

    fun openAction(context: Context, type: String, value: String) {
        when (type) {
            "URL" -> {
                val url = if (value.startsWith("http")) value else "https://$value"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            "Email" -> context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$value")))
            "Phone" -> {
                val digits = value.replace(Regex("[^0-9+]"), "")
                if (digits.isNotEmpty()) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                }
            }
        }
    }
}
