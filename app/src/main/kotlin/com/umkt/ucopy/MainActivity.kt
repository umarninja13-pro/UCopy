package com.umkt.ucopy

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import com.umkt.ucopy.data.UserPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.service.ClipboardListenerService
import com.umkt.ucopy.ui.home.HomeScreen
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.ui.theme.UCopyTheme
import com.umkt.ucopy.ui.theme.TimeBasedThemeConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.umkt.ucopy.ui.widget.NoteWidgetProvider
import com.umkt.ucopy.R

class MainActivity : AppCompatActivity() {

    private var _sharedItemToOpen by mutableStateOf<Clip?>(null)

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            val sharedSubject = intent.getStringExtra(android.content.Intent.EXTRA_SUBJECT)
            if (!sharedText.isNullOrBlank()) {
                _sharedItemToOpen = Clip(
                    id = "",
                    title = sharedSubject ?: sharedText.lines().firstOrNull()?.take(60) ?: getString(R.string.shared_note),
                    content = sharedText,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
        val clipJson = intent.getStringExtra("open_clip")
        if (!clipJson.isNullOrBlank()) {
            try {
                _sharedItemToOpen = json.decodeFromString(clipJson)
            } catch (_: Exception) { }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private val THEME_PREF_KEY = "app_theme_mode"
    private val DYNAMIC_COLOR_PREF_KEY = "dynamic_color_enabled"
    private val DESIGN_MODE_PREF_KEY = "design_mode"
    private val ACTION_PREF_KEY = "action_prefs"
    private val LISTENER_PREF_KEY = "clipboard_listener"
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        val prefs = UserPreferences(this)

        // Service state is managed by the service itself now for faster startup

        val savedTheme = prefs.getString(THEME_PREF_KEY, ThemeMode.SYSTEM.name)
        val initialTheme = try { ThemeMode.valueOf(savedTheme) } catch (e: Exception) { ThemeMode.SYSTEM }
        val initialDynamicColor = prefs.getBoolean(DYNAMIC_COLOR_PREF_KEY, true)
        val savedDesign = prefs.getString(DESIGN_MODE_PREF_KEY, DesignMode.DEFAULT.name)
        val initialDesignMode = try {
            com.umkt.ucopy.ui.design.DesignMode.valueOf(savedDesign)
        } catch (e: Exception) {
            DesignMode.DEFAULT
        }

        val clipJson = intent.getStringExtra("open_clip")
        var initialClipToOpen: Clip? = null
        if (!clipJson.isNullOrBlank()) {
            try {
                initialClipToOpen = json.decodeFromString(clipJson)
            } catch (_: Exception) { }
        }

        enableEdgeToEdge()

        setContent {
            var selectedThemeMode by remember { mutableStateOf(initialTheme) }
            var configVersion by remember { mutableStateOf(0) }
            var dynamicColorEnabled by remember { mutableStateOf(initialDynamicColor) }
            var selectedDesignMode by remember { mutableStateOf(initialDesignMode) }
            var actionPrefs by remember { 
                mutableStateOf(
                    com.umkt.ucopy.ui.design.ActionPreferences(
                        showCopy = prefs.getBoolean("show_copy", true),
                        showShare = prefs.getBoolean("show_share", true),
                        markdownPreview = prefs.getBoolean("markdown_preview", true),
                        htmlPreview = prefs.getBoolean("html_preview", true),
                        showOpeningAnimation = prefs.getBoolean("show_opening_animation", true),
                        showListAnimations = prefs.getBoolean("show_list_animations", true)
                    )
                ) 
            }
            var clipToOpen by remember { mutableStateOf(initialClipToOpen) }

            val view = LocalView.current
            val window = (view.context as? Activity)?.window

            val themeModeToUse = remember(selectedThemeMode, configVersion) {
                if (selectedThemeMode == ThemeMode.TIME_BASED) {
                    val config = TimeBasedThemeConfig.load(this@MainActivity)
                    if (config.enabled) {
                        val currentTime = java.time.LocalTime.now()
                        config.getEffectiveTheme(currentTime)
                    } else {
                        ThemeMode.SYSTEM
                    }
                } else {
                    selectedThemeMode
                }
            }

            LaunchedEffect(selectedThemeMode) {
                while (true) {
                    delay(60000)
                    if (selectedThemeMode == ThemeMode.TIME_BASED) {
                        configVersion++
                    }
                }
            }

            val effectiveDark = when (themeModeToUse) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AMOLED -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }

            androidx.compose.runtime.CompositionLocalProvider(
                com.umkt.ucopy.ui.design.LocalActionPreferences provides actionPrefs
            ) {
            UCopyTheme(
                themeMode = themeModeToUse,
                dynamicColor = dynamicColorEnabled,
                designMode = selectedDesignMode
            ) {
                val appBackground = MaterialTheme.colorScheme.background
                val targetDecorColor = when {
                    !effectiveDark -> android.graphics.Color.WHITE
                    themeModeToUse == ThemeMode.AMOLED -> android.graphics.Color.BLACK
                    else -> android.graphics.Color.rgb(0x12, 0x12, 0x12)
                }
                SideEffect {
                    window?.let { w ->
                        @Suppress("DEPRECATION")
                        w.statusBarColor = android.graphics.Color.TRANSPARENT
                        @Suppress("DEPRECATION")
                        w.navigationBarColor = appBackground.toArgb()
                        w.decorView.setBackgroundColor(targetDecorColor)
                        val controller = WindowInsetsControllerCompat(w, view)
                        controller.isAppearanceLightStatusBars = !effectiveDark
                        controller.isAppearanceLightNavigationBars = !effectiveDark
                    }
                }

                val viewModel: ClipViewModel = viewModel()
                HomeScreen(
                    viewModel = viewModel,
                    themeMode = selectedThemeMode,
                    dynamicColorEnabled = dynamicColorEnabled,
                    onThemeChange = { newTheme ->
                        selectedThemeMode = newTheme
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString(THEME_PREF_KEY, newTheme.name)
                            .apply()
                    },
                    onThemeConfigSaved = {
                        configVersion++
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString(THEME_PREF_KEY, ThemeMode.TIME_BASED.name)
                            .apply()
                    },
                    onDynamicColorToggle = { enabled ->
                        dynamicColorEnabled = enabled
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean(DYNAMIC_COLOR_PREF_KEY, enabled)
                            .apply()
                    },
                    designMode = selectedDesignMode,
                    onDesignChange = { newDesign ->
                        selectedDesignMode = newDesign
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString(DESIGN_MODE_PREF_KEY, newDesign.name)
                            .apply()
                    },
                    clipToOpen = clipToOpen,
                    onClipOpened = { clipToOpen = null },
                    sharedItemToOpen = _sharedItemToOpen,
                    onSharedItemOpened = { _sharedItemToOpen = null },
                    actionPrefs = actionPrefs,
                    onActionPrefsChange = { newPrefs ->
                        actionPrefs = newPrefs
                        prefs.putBoolean("show_copy", newPrefs.showCopy)
                        prefs.putBoolean("show_share", newPrefs.showShare)
                        prefs.putBoolean("markdown_preview", newPrefs.markdownPreview)
                        prefs.putBoolean("html_preview", newPrefs.htmlPreview)
                        prefs.putBoolean("show_opening_animation", newPrefs.showOpeningAnimation)
                        prefs.putBoolean("show_list_animations", newPrefs.showListAnimations)
                    }
                )
            }
            }
        }
    }

    @Composable
    private fun isSystemInDarkTheme(): Boolean {
        return android.content.res.Configuration.UI_MODE_NIGHT_YES ==
                LocalView.current.context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
    }

    override fun onResume() {
        super.onResume()
        ClipboardListenerService.isAppVisible = true
        
        // Force widget update when app opens
        val manager = android.appwidget.AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(android.content.ComponentName(this, com.umkt.ucopy.ui.widget.NoteWidgetProvider::class.java))
        for (id in ids) {
            com.umkt.ucopy.ui.widget.NoteWidgetProvider.updateWidget(this, manager, id)
        }
        
        // Force check clipboard when app opens
        val prefs = UserPreferences(this)
        if (prefs.getBoolean("clipboard_listener", false)) {
            val intent = android.content.Intent(this, ClipboardListenerService::class.java)
            intent.action = ClipboardListenerService.ACTION_CHECK_CLIPBOARD
            startService(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        ClipboardListenerService.isAppVisible = false
    }
}