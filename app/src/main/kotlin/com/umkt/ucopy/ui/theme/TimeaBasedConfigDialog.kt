package com.umkt.ucopy.ui.theme

import com.umkt.ucopy.R

import androidx.compose.ui.res.stringResource

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBasedConfigDialog(
    onDismiss: () -> Unit,
    onConfigSaved: () -> Unit
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(TimeBasedThemeConfig.load(context)) }

    var showDayPicker by remember { mutableStateOf(false) }
    var showNightPicker by remember { mutableStateOf(false) }
    
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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = stringResource(R.string.time_based_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.time_based_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enable Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.enable_schedule),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.switch_themes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { config = config.copy(enabled = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (config.enabled) {
                Spacer(modifier = Modifier.height(24.dp))

                // Day Theme Section
                Text(
                    text = stringResource(R.string.day_theme),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = config.dayTheme == ThemeMode.LIGHT,
                        onClick = { config = config.copy(dayTheme = ThemeMode.LIGHT) },
                        label = { Text(stringResource(R.string.light_mode), modifier = Modifier.padding(horizontal = 8.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Night Theme Section
                Text(
                    text = stringResource(R.string.night_theme),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = config.nightTheme == ThemeMode.DARK,
                        onClick = { config = config.copy(nightTheme = ThemeMode.DARK) },
                        label = { Text(stringResource(R.string.dark_mode), modifier = Modifier.padding(horizontal = 8.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = config.nightTheme == ThemeMode.AMOLED,
                        onClick = { config = config.copy(nightTheme = ThemeMode.AMOLED) },
                        label = { Text(stringResource(R.string.amoled), modifier = Modifier.padding(horizontal = 8.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Day Start Time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.day_starts),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDayPicker = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%02d:%02d", config.dayStartHour, config.dayStartMinute),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Night Start Time
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.night_starts),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showNightPicker = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%02d:%02d", config.nightStartHour, config.nightStartMinute),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.schedule_info, config.dayStart.toString(), config.nightStart.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        TimeBasedThemeConfig.save(context, config)
                        onConfigSaved()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save_schedule), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Native Time Pickers
    if (showDayPicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                config = config.copy(dayStartHour = hour, dayStartMinute = minute)
                showDayPicker = false
            },
            config.dayStartHour,
            config.dayStartMinute,
            true
        ).show()
    }

    if (showNightPicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                config = config.copy(nightStartHour = hour, nightStartMinute = minute)
                showNightPicker = false
            },
            config.nightStartHour,
            config.nightStartMinute,
            true
        ).show()
    }
}
