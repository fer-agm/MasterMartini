package cl.mastermartini.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("settings")

data class Prefs(
    val userId: Long = -1,
    val textScale: Float = 1f,
    val highContrast: Boolean = false,
    val haptics: Boolean = true
)

class SettingsStore(private val ctx: Context) {
    private val kUser = longPreferencesKey("user")
    private val kScale = floatPreferencesKey("scale")
    private val kContrast = booleanPreferencesKey("contrast")
    private val kHaptics = booleanPreferencesKey("haptics")

    val prefs: Flow<Prefs> = ctx.ds.data.map {
        Prefs(it[kUser] ?: -1, it[kScale] ?: 1f, it[kContrast] ?: false, it[kHaptics] ?: true)
    }

    suspend fun setUser(id: Long) { ctx.ds.edit { it[kUser] = id } }
    suspend fun setScale(v: Float) { ctx.ds.edit { it[kScale] = v } }
    suspend fun setContrast(v: Boolean) { ctx.ds.edit { it[kContrast] = v } }
    suspend fun setHaptics(v: Boolean) { ctx.ds.edit { it[kHaptics] = v } }
}
