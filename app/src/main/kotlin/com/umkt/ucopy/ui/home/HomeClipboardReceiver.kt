package com.umkt.ucopy.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.service.ClipboardListenerService
import com.umkt.ucopy.ui.state.ClipViewModel
import kotlinx.serialization.json.Json

@Composable
fun rememberClipboardReceiver(
    context: Context,
    viewModel: ClipViewModel,
    prefs: UserPreferences,
    onListenerStateChange: (Boolean) -> Unit,
    onNewClipDetected: (String) -> Unit
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ClipboardListenerService.BROADCAST_ACTION -> {
                        val running = intent.getBooleanExtra(ClipboardListenerService.EXTRA_IS_RUNNING, false)
                        onListenerStateChange(running)
                        prefs.putBoolean("clipboard_listener", running)
                    }
                    ClipboardListenerService.ACTION_NEW_CLIP_DETECTED -> {
                        val text = intent.getStringExtra(ClipboardListenerService.EXTRA_NEW_CLIP_TEXT)
                        if (!text.isNullOrBlank()) {
                            onNewClipDetected(text)
                        }
                    }
                    ClipboardListenerService.ACTION_CLIP_SAVED -> {
                        val clipJson = intent.getStringExtra(ClipboardListenerService.EXTRA_CLIP_ITEM)
                        if (!clipJson.isNullOrBlank()) {
                            try {
                                val item = json.decodeFromString<Clip>(clipJson)
                                viewModel.addItemDirectly(item)
                            } catch (e: Exception) {
                                viewModel.loadItems()
                            }
                        } else {
                            viewModel.loadItems()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(ClipboardListenerService.BROADCAST_ACTION)
            addAction(ClipboardListenerService.ACTION_CLIP_SAVED)
            addAction(ClipboardListenerService.ACTION_NEW_CLIP_DETECTED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}