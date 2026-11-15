package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LayoutMode
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hephaistos_prefs")

class ThemePreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_LAYOUT = stringPreferencesKey("default_layout")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val defaultLayout: Flow<LayoutMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.DEFAULT_LAYOUT] ?: LayoutMode.LIST.name
        runCatching { LayoutMode.valueOf(raw) }.getOrDefault(LayoutMode.LIST)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultLayout(layout: LayoutMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_LAYOUT] = layout.name
        }
    }
}
