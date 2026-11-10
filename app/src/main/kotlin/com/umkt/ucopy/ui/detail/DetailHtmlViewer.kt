package com.umkt.ucopy.ui.detail

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DetailHtmlViewer(content: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                addJavascriptInterface(WebAppInterface(context), "Android")
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val js = "javascript:(function() {" +
                                "  var clipboard = navigator.clipboard;" +
                                "  if (clipboard) {" +
                                "    navigator.clipboard.writeText = function(text) {" +
                                "      if (typeof Android !== 'undefined' && Android.copyToClipboard) {" +
                                "        Android.copyToClipboard(text);" +
                                "        return Promise.resolve();" +
                                "      }" +
                                "      return clipboard.writeText(text);" +
                                "    }" +
                                "  }" +
                                "})()"
                        view?.evaluateJavascript(js, null)
                    }
                }
                loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
        }
    )
}