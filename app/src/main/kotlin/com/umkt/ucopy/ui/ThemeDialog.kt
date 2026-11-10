package com.umkt.ucopy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.R
import com.umkt.ucopy.ui.theme.ThemeMode
import com.umkt.ucopy.ui.theme.TimeBasedConfigDialog
import com.umkt.ucopy.ui.theme.TimeBasedThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDialog(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    onThemeConfigSaved: () -> Unit = {},
    onDynamicColorToggle: (Boolean) -> Unit
) {
    var showTimeConfig by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.app_theme),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.app_theme_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            ThemeMode.values().forEach { mode ->
                val isSelected = themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onThemeChange(mode)
                            if (mode != ThemeMode.TIME_BASED) {
                                onDismiss() 
                            }
                        }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                    Icon(
                        imageVector = when(mode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.AMOLED -> Icons.Default.Brightness4
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            ThemeMode.TIME_BASED -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp).size(24.dp)
                    )
                    Text(
                        text = when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.light_mode)
                            ThemeMode.DARK -> stringResource(R.string.dark_mode)
                            ThemeMode.AMOLED -> stringResource(R.string.amoled)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.TIME_BASED -> stringResource(R.string.time_based_custom)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (mode == ThemeMode.TIME_BASED) {
                        IconButton(
                            onClick = { showTimeConfig = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.configure_times),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dynamic_color),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.dynamic_color_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (themeMode == ThemeMode.TIME_BASED) {
                Spacer(modifier = Modifier.height(16.dp))
                val config = TimeBasedThemeConfig.load(context)
                if (config.enabled) {
                    Text(
                        text = "Day ${config.dayStart} - ${config.nightStart} | ${config.dayTheme.name} → ${config.nightTheme.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        text = "⏰ Time-based mode is disabled. Tap Settings to configure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    if (showTimeConfig) {
        TimeBasedConfigDialog(
            onDismiss = { showTimeConfig = false },
            onConfigSaved = {
                onThemeChange(ThemeMode.TIME_BASED)
                onThemeConfigSaved()
                showTimeConfig = false
            }
        )
    }
}
