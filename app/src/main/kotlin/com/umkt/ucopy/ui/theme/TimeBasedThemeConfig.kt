package com.umkt.ucopy.ui.theme

import android.content.Context
import java.time.LocalTime

data class TimeBasedThemeConfig(
    val enabled: Boolean = false,
    val dayStartHour: Int = 6,
    val dayStartMinute: Int = 0,
    val nightStartHour: Int = 18,
    val nightStartMinute: Int = 0,
    val dayTheme: ThemeMode = ThemeMode.LIGHT,
    val nightTheme: ThemeMode = ThemeMode.DARK
) {
    val dayStart: LocalTime get() = LocalTime.of(dayStartHour, dayStartMinute)
    val nightStart: LocalTime get() = LocalTime.of(nightStartHour, nightStartMinute)

    fun getEffectiveTheme(currentTime: LocalTime): ThemeMode {
        val day = dayStart
        val night = nightStart

        val result = if (day < night) {
            if (currentTime >= day && currentTime < night) dayTheme else nightTheme
        } else {
            if (currentTime >= day || currentTime < night) dayTheme else nightTheme
        }
        android.util.Log.d("TimeBased", "currentTime=$currentTime, day=$day, night=$night, result=$result")
        return result
    }

    companion object {
        fun load(context: Context): TimeBasedThemeConfig {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            return TimeBasedThemeConfig(
                enabled = prefs.getBoolean("time_based_enabled", false),
                dayStartHour = prefs.getInt("time_based_day_hour", 6),
                dayStartMinute = prefs.getInt("time_based_day_minute", 0),
                nightStartHour = prefs.getInt("time_based_night_hour", 18),
                nightStartMinute = prefs.getInt("time_based_night_minute", 0),
                dayTheme = try {
                    ThemeMode.valueOf(prefs.getString("time_based_day_theme", ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name)
                } catch (e: Exception) { ThemeMode.LIGHT },
                nightTheme = try {
                    ThemeMode.valueOf(prefs.getString("time_based_night_theme", ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
                } catch (e: Exception) { ThemeMode.DARK }
            )
        }

        fun save(context: Context, config: TimeBasedThemeConfig) {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("time_based_enabled", config.enabled)
                putInt("time_based_day_hour", config.dayStartHour)
                putInt("time_based_day_minute", config.dayStartMinute)
                putInt("time_based_night_hour", config.nightStartHour)
                putInt("time_based_night_minute", config.nightStartMinute)
                putString("time_based_day_theme", config.dayTheme.name)
                putString("time_based_night_theme", config.nightTheme.name)
                apply()
            }
        }
    }
}