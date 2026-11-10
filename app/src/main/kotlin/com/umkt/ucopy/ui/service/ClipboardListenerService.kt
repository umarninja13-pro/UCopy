package com.umkt.ucopy.ui.service

import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.umkt.ucopy.R
import com.umkt.ucopy.MainActivity
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.model.Clip
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ClipboardListenerService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var lastClipHash: Int = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var periodicCheckRunnable: Runnable? = null
    private var isListenerAdded = false
    private var isPeriodicCheckRunning = false
    private var cachedItems: List<Clip> = emptyList()
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5000L
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val CHANNEL_ID = "clipboard_listener_channel"
        const val CHANNEL_FOREGROUND_ID = "clipboard_listener_foreground"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "STOP"
        const val ACTION_COPY = "COPY"
        const val ACTION_SAVE = "SAVE"
        const val ACTION_DISMISS = "DISMISS"
        const val EXTRA_CLIP_TEXT = "clip_text"
        const val EXTRA_CLIP_SOURCE = "clip_source"
        const val BROADCAST_ACTION = "CLIPBOARD_LISTENER_STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "isRunning"
        const val ACTION_CLIP_SAVED = "CLIP_SAVED"
        const val EXTRA_CLIP_ITEM = "clip_item"
        const val ACTION_NEW_CLIP_DETECTED = "NEW_CLIP_DETECTED"
        const val ACTION_CHECK_CLIPBOARD = "CHECK_CLIPBOARD"
        const val EXTRA_NEW_CLIP_TEXT = "new_clip_text"
        
        var isAppVisible = false

        fun start(context: Context) {
            val intent = Intent(context, ClipboardListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            context.sendBroadcast(Intent(BROADCAST_ACTION).putExtra(EXTRA_IS_RUNNING, true))
        }

        fun stop(context: Context) {
            val intent = Intent(context, ClipboardListenerService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        initLastClipHash()
        refreshCache()
    }

    private fun refreshCache() {
        serviceScope.launch {
            try {
                val repo = ClipRepository(applicationContext)
                cachedItems = repo.getAll()
                cacheTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
            }
        }
    }

    private fun getCachedItems(): List<Clip> {
        val now = System.currentTimeMillis()
        if (now - cacheTimestamp > CACHE_DURATION_MS) {
            refreshCache()
        }
        return cachedItems
    }

    private fun initLastClipHash() {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val text = item.text?.toString()
                if (!text.isNullOrBlank()) {
                    lastClipHash = text.hashCode()
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopService()
                return START_NOT_STICKY
            }
            ACTION_COPY -> {
                val text = intent.getStringExtra(EXTRA_CLIP_TEXT) ?: ""
                val clip = ClipData.newPlainText("U Copy", text)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.copied_back), Toast.LENGTH_SHORT).show()
                dismissNotification()
                return START_STICKY
            }
            ACTION_SAVE -> {
                val text = intent.getStringExtra(EXTRA_CLIP_TEXT) ?: ""
                val source = intent.getStringExtra(EXTRA_CLIP_SOURCE) ?: ""
                saveClipToRepository(text, source)
                dismissNotification()
                return START_STICKY
            }
            ACTION_DISMISS -> {
                dismissNotification()
                return START_STICKY
            }
            ACTION_CHECK_CLIPBOARD -> {
                checkClipboard(force = true)
                return START_STICKY
            }
        }

        val prefs = UserPreferences(this)
        prefs.putBoolean("clipboard_listener", true)
        sendBroadcast(Intent(BROADCAST_ACTION).putExtra(EXTRA_IS_RUNNING, true))

        startForeground(NOTIFICATION_ID, createPersistentNotification())

        startListener()
        startPeriodicCheck()

        return START_STICKY
    }

    private fun startListener() {
        if (isListenerAdded) return
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            handleClipboardChange()
        }
        listener?.let {
            clipboardManager.addPrimaryClipChangedListener(it)
            isListenerAdded = true
        }
    }

    private fun stopListener() {
        if (!isListenerAdded) return
        listener?.let {
            clipboardManager.removePrimaryClipChangedListener(it)
            listener = null
            isListenerAdded = false
        }
    }

    private fun startPeriodicCheck() {
        if (isPeriodicCheckRunning) return
        stopPeriodicCheck()
        periodicCheckRunnable = object : Runnable {
            override fun run() {
                checkClipboard()
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(periodicCheckRunnable!!)
        isPeriodicCheckRunning = true
    }

    private fun stopPeriodicCheck() {
        periodicCheckRunnable?.let {
            handler.removeCallbacks(it)
            periodicCheckRunnable = null
            isPeriodicCheckRunning = false
        }
    }

    private fun checkClipboard(force: Boolean = false) {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val text = item.text?.toString() ?: ""
                if (text.isNotBlank()) {
                    val hash = text.hashCode()
                    // If force is true, we bypass the hash check so it shows the popup again
                    if (hash != lastClipHash || force) {
                        lastClipHash = hash
                        if (isAppVisible) {
                            val source = clip.description?.label?.toString() ?: "Unknown"
                            handleClipboardContent(text, source)
                        } else {
                            val prefs = UserPreferences(this)
                            if (prefs.getBoolean("clipboard_listener", false)) {
                                showGenericClipNotification()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun handleClipboardChange() {
        checkClipboard(force = true)
    }

    private fun handleClipboardContent(text: String, source: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = getCachedItems()
                val alreadySaved = items.any { it.content == text }

                withContext(Dispatchers.Main) {
                    if (alreadySaved) {
                        return@withContext
                    }
                    
                    if (isAppVisible) {
                        // App is open, send broadcast for popup
                        val intent = Intent(ACTION_NEW_CLIP_DETECTED).apply {
                            putExtra(EXTRA_NEW_CLIP_TEXT, text)
                            setPackage(packageName)
                        }
                        sendBroadcast(intent)
                    } else {
                        // App is closed, show notification
                        showSaveNotification(text, source)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAppVisible) {
                        showSaveNotification(text, source)
                    }
                }
            }
        }
    }

    private fun showSaveNotification(text: String, source: String) {
        val openIntent = Intent(this, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        openIntent.putExtra("clip_text", text)
        openIntent.putExtra("clip_source", source)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(this, ClipboardListenerService::class.java)
        saveIntent.action = ACTION_SAVE
        saveIntent.putExtra(EXTRA_CLIP_TEXT, text)
        saveIntent.putExtra(EXTRA_CLIP_SOURCE, source)
        val pendingSaveIntent = PendingIntent.getService(
            this, 1, saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val copyIntent = Intent(this, ClipboardListenerService::class.java)
        copyIntent.action = ACTION_COPY
        copyIntent.putExtra(EXTRA_CLIP_TEXT, text)
        val pendingCopyIntent = PendingIntent.getService(
            this, 2, copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, ClipboardListenerService::class.java)
        dismissIntent.action = ACTION_DISMISS
        val pendingDismissIntent = PendingIntent.getService(
            this, 3, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.new_clip_notif_title))
            .setContentText(getString(R.string.from_source_format, source))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.take(200)))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(pendingOpenIntent)
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save, getString(R.string.save_action), pendingSaveIntent).build())
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_share, getString(R.string.copy_action), pendingCopyIntent).build())
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.dismiss_action), pendingDismissIntent).build())
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)
    }


    private fun showGenericClipNotification() {
        val openIntent = Intent(this, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.new_clip_detected))
            .setContentText(getString(R.string.tap_to_open_save))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpenIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 3, notification)
    }

    private fun dismissNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID + 1)
    }

    private fun saveClipToRepository(text: String, source: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ClipRepository(applicationContext)
                val items = getCachedItems()
                val alreadySaved = items.any { it.content == text }

                if (alreadySaved) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ClipboardListenerService,
                            getString(R.string.clip_already_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val title = text.lines().firstOrNull()?.take(60) ?: "Untitled"
                val item = Clip(
                    id = UUID.randomUUID().toString(),
                    title = title.ifEmpty { "Untitled" },
                    content = text,
                    timestamp = System.currentTimeMillis()
                )
                repo.add(item)
                cachedItems = listOf(item) + cachedItems
                cacheTimestamp = System.currentTimeMillis()

                val jsonString = json.encodeToString(item)
                val broadcastIntent = Intent(ACTION_CLIP_SAVED).apply {
                    putExtra(EXTRA_CLIP_ITEM, jsonString)
                }
                sendBroadcast(broadcastIntent)

                val openDetailIntent = Intent(this@ClipboardListenerService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("open_clip", jsonString)
                }
                val pendingDetailIntent = PendingIntent.getActivity(
                    this@ClipboardListenerService, 5, openDetailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                withContext(Dispatchers.Main) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notification = NotificationCompat.Builder(
                        this@ClipboardListenerService, CHANNEL_ID
                    )
                        .setContentTitle(getString(R.string.clip_saved_title))
                        .setContentText(getString(R.string.tap_to_open_text))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setAutoCancel(true)
                        .setContentIntent(pendingDetailIntent)
                        .build()
                    nm.notify(NOTIFICATION_ID + 2, notification)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val highChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.clipboard_listener_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitors clipboard for new content"
                setShowBadge(false)
            }
            nm.createNotificationChannel(highChannel)
            val lowChannel = NotificationChannel(
                CHANNEL_FOREGROUND_ID,
                getString(R.string.clipboard_service_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while clipboard listener runs"
                setShowBadge(false)
            }
            nm.createNotificationChannel(lowChannel)
        }
    }

    private fun createPersistentNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ClipboardListenerService::class.java)
        stopIntent.action = ACTION_STOP
        val pendingStopIntent = PendingIntent.getService(
            this, 4, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_FOREGROUND_ID)
            .setContentTitle("U Copy")
            .setContentText(getString(R.string.listener_active_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenIntent)
            .addAction(NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_btn), pendingStopIntent).build())
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun stopService() {
        stopListener()
        stopPeriodicCheck()

        val prefs = UserPreferences(this)
        prefs.putBoolean("clipboard_listener", false)

        sendBroadcast(Intent(BROADCAST_ACTION).putExtra(EXTRA_IS_RUNNING, false))

        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListener()
        stopPeriodicCheck()
        serviceScope.cancel()
        super.onDestroy()
    }
}