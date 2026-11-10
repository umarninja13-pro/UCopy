package com.umkt.ucopy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object PreferencesCache {
    val map = ConcurrentHashMap<String, Any>()
    @Volatile var loaded = false
}

class UserPreferences(private val context: Context) {

    init {
        if (!PreferencesCache.loaded) {
            synchronized(PreferencesCache) {
                if (!PreferencesCache.loaded) {
                    runBlocking(Dispatchers.IO) {
                        try {
                            migrateFromSharedPrefs()
                            val prefs = context.dataStore.data.first()
                            prefs.asMap().forEach { (key, value) ->
                                PreferencesCache.map[key.name] = value
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    PreferencesCache.loaded = true
                }
            }
        }
    }

    private suspend fun migrateFromSharedPrefs() {
        val oldPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!oldPrefs.getBoolean("datastore_migrated", false)) {
            val allPrefs = oldPrefs.all
            context.dataStore.edit { mutablePrefs ->
                allPrefs.forEach { (key, value) ->
                    when (value) {
                        is String -> mutablePrefs[stringPreferencesKey(key)] = value
                        is Boolean -> mutablePrefs[booleanPreferencesKey(key)] = value
                        is Int -> mutablePrefs[intPreferencesKey(key)] = value
                        is Long -> mutablePrefs[longPreferencesKey(key)] = value
                        is Float -> mutablePrefs[floatPreferencesKey(key)] = value
                        is Set<*> -> {
                            val stringSet = value.mapNotNull { it as? String }.toSet()
                            mutablePrefs[stringSetPreferencesKey(key)] = stringSet
                        }
                    }
                }
                mutablePrefs[booleanPreferencesKey("datastore_migrated")] = true
            }
        }
    }

    fun getString(key: String, default: String): String = PreferencesCache.map[key] as? String ?: default
    fun putString(key: String, value: String) {
        PreferencesCache.map[key] = value
        runBlocking(Dispatchers.IO) { context.dataStore.edit { it[stringPreferencesKey(key)] = value } }
    }

    fun getBoolean(key: String, default: Boolean): Boolean = PreferencesCache.map[key] as? Boolean ?: default
    fun putBoolean(key: String, value: Boolean) {
        PreferencesCache.map[key] = value
        runBlocking(Dispatchers.IO) { context.dataStore.edit { it[booleanPreferencesKey(key)] = value } }
    }

    fun getInt(key: String, default: Int): Int = PreferencesCache.map[key] as? Int ?: default
    fun putInt(key: String, value: Int) {
        PreferencesCache.map[key] = value
        runBlocking(Dispatchers.IO) { context.dataStore.edit { it[intPreferencesKey(key)] = value } }
    }

    fun getStringSet(key: String, default: Set<String>): Set<String> {
        @Suppress("UNCHECKED_CAST")
        return PreferencesCache.map[key] as? Set<String> ?: default
    }
    fun putStringSet(key: String, value: Set<String>) {
        PreferencesCache.map[key] = value
        runBlocking(Dispatchers.IO) { context.dataStore.edit { it[stringSetPreferencesKey(key)] = value } }
    }

    fun getStringOrNull(key: String): String? = PreferencesCache.map[key] as? String
    
    fun remove(key: String) {
        PreferencesCache.map.remove(key)
        runBlocking(Dispatchers.IO) { context.dataStore.edit { it.remove(stringPreferencesKey(key)) } }
    }

    fun getAll(): Map<String, Any?> = PreferencesCache.map.toMap()

    suspend fun getStringAsync(key: String, default: String): String {
        return getString(key, default)
    }

    suspend fun getBooleanAsync(key: String, default: Boolean): Boolean {
        return getBoolean(key, default)
    }

    suspend fun getStringOrNullAsync(key: String): String? {
        return getStringOrNull(key)
    }
}
