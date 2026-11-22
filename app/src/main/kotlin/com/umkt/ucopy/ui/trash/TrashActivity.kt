package com.umkt.ucopy.ui.trash

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.umkt.ucopy.R
import com.umkt.ucopy.data.ClipRepository
import com.umkt.ucopy.data.UserPreferences
import com.umkt.ucopy.model.Clip
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.ui.theme.UCopyTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs = remember { UserPreferences(this@TrashActivity) }
            val themeName = prefs.getString("app_theme_mode", ThemeMode.SYSTEM.name)
            val themeMode = try { ThemeMode.valueOf(themeName) } catch (_: Exception) { ThemeMode.SYSTEM }
            val designName = prefs.getString("design_mode", DesignMode.DEFAULT.name)
            val designMode = try { DesignMode.valueOf(designName) } catch (_: Exception) { DesignMode.DEFAULT }
            val dynamicColor = prefs.getBoolean("dynamic_color_enabled", true)

            UCopyTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                designMode = designMode
            ) {
                val view = LocalView.current
                val window = (view.context as? Activity)?.window
                val effectiveDark = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK, ThemeMode.AMOLED -> true
                    else -> isSystemInDarkTheme()
                }
                val appBackground = MaterialTheme.colorScheme.background

                SideEffect {
                    window?.let { w ->
                        @Suppress("DEPRECATION")
                        w.statusBarColor = android.graphics.Color.TRANSPARENT
                        @Suppress("DEPRECATION")
                        w.navigationBarColor = android.graphics.Color.TRANSPARENT
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            w.isNavigationBarContrastEnforced = false
                        }
                        val controller = WindowInsetsControllerCompat(w, view)
                        controller.isAppearanceLightStatusBars = !effectiveDark
                        controller.isAppearanceLightNavigationBars = !effectiveDark
                    }
                }

                TrashScreen(onBack = { finish() })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ClipRepository(context) }
    val scope = rememberCoroutineScope()

    val itemsState by produceState<List<Clip>?>(initialValue = null) {
        repository.getTrashFlow().collect { value = it }
    }
    val loading = itemsState == null
    val items = itemsState ?: emptyList()

    var showEmptyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.purgeOldTrash()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.empty_trash))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            stringResource(R.string.trash_auto_delete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(items, key = { it.id }) { clip ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        clip.title.ifEmpty { stringResource(R.string.untitled) },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        clip.content.take(100).replace("\n", " ").ifEmpty { stringResource(R.string.no_content) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    clip.deletedAt?.let { t ->
                                        Text(
                                            dateFormat.format(Date(t)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        repository.restoreFromTrash(clip.id)
                                        ClipRepository.dataChangedFromTrash = true
                                    }
                                }) {
                                    Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.restore), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        repository.deletePermanently(clip.id)
                                        ClipRepository.dataChangedFromTrash = true
                                    }
                                }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.empty_trash)) },
            text = { Text(stringResource(R.string.empty_trash_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        repository.emptyTrash()
                        ClipRepository.dataChangedFromTrash = true
                    }
                    showEmptyConfirm = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
