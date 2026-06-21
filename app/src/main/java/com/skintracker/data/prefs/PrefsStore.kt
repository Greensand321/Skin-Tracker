package com.skintracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skintracker.data.SnapshotMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keto_prefs")
private val prefsJson = Json { ignoreUnknownKeys = true }

class PrefsStore(context: Context) {

    private val ds: DataStore<Preferences> = context.dataStore

    // ── Theme ────────────────────────────────────────────────────────────────

    val theme: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[THEME_KEY] ?: "midnight" }

    suspend fun setTheme(id: String) {
        ds.edit { prefs -> prefs[THEME_KEY] = id }
    }

    /**
     * Auto-theme preferences — native counterpart to the web app's
     * `kt_theme_auto`/`kt_theme_dark_auto`/`kt_theme_light_auto` (CLAUDE.md
     * "Theme System"). When enabled, the active theme tracks the system's
     * dark/light setting via [com.skintracker.ui.theme.resolveAutoTheme]
     * instead of the single manually-picked [theme].
     */
    val autoThemeEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[AUTO_ENABLED_KEY] ?: false }

    val darkAutoTheme: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[DARK_AUTO_KEY] ?: "midnight" }

    val lightAutoTheme: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[LIGHT_AUTO_KEY] ?: "ivory" }

    suspend fun setAutoThemeEnabled(enabled: Boolean) {
        ds.edit { prefs -> prefs[AUTO_ENABLED_KEY] = enabled }
    }

    suspend fun setDarkAutoTheme(id: String) {
        ds.edit { prefs -> prefs[DARK_AUTO_KEY] = id }
    }

    suspend fun setLightAutoTheme(id: String) {
        ds.edit { prefs -> prefs[LIGHT_AUTO_KEY] = id }
    }

    // ── Snapshots ────────────────────────────────────────────────────────────

    /** Persisted list of up to 25 snapshot metadata entries (CLAUDE.md "Snapshots"). */
    val snapshots: Flow<List<SnapshotMeta>> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val s = prefs[SNAPSHOTS_KEY] ?: return@map emptyList()
            runCatching {
                prefsJson.decodeFromString(ListSerializer(SnapshotMeta.serializer()), s)
            }.getOrElse { emptyList() }
        }

    suspend fun setSnapshots(snaps: List<SnapshotMeta>) {
        ds.edit { prefs ->
            prefs[SNAPSHOTS_KEY] = prefsJson.encodeToString(ListSerializer(SnapshotMeta.serializer()), snaps)
        }
    }

    // ── Quick-select ─────────────────────────────────────────────────────────

    /**
     * Persisted food chip list for QuickSelectSheet, or `null` if the user has
     * never customized it. `null` is distinct from an empty list: `null` means
     * "fall back to defaults", while `[]` means the user deliberately removed
     * every item and that choice should stick (see AppViewModel.quickSelectItems).
     * Corrupt/unparsable data is treated the same as "never customized".
     */
    val quickSelectItems: Flow<List<String>?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val s = prefs[QUICK_SELECT_KEY] ?: return@map null
            runCatching {
                prefsJson.decodeFromString(ListSerializer(String.serializer()), s)
            }.getOrNull()
        }

    suspend fun setQuickSelectItems(items: List<String>) {
        ds.edit { prefs ->
            prefs[QUICK_SELECT_KEY] = prefsJson.encodeToString(ListSerializer(String.serializer()), items)
        }
    }

    // ── Periodic backup ───────────────────────────────────────────────────────

    val backupEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[BACKUP_ENABLED_KEY] ?: false }

    suspend fun setBackupEnabled(enabled: Boolean) {
        ds.edit { prefs -> prefs[BACKUP_ENABLED_KEY] = enabled }
    }

    /** "daily" or "weekly" — matches the string values used by BackupWorker.schedule(). */
    val backupFrequency: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[BACKUP_FREQ_KEY] ?: "daily" }

    suspend fun setBackupFrequency(freq: String) {
        ds.edit { prefs -> prefs[BACKUP_FREQ_KEY] = freq }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    val notificationsEnabled: Flow<Boolean> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[NOTIF_ENABLED_KEY] ?: false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        ds.edit { prefs -> prefs[NOTIF_ENABLED_KEY] = enabled }
    }

    /** Hour of day (0–23) for the daily reminder. Default 20 = 8 PM. */
    val notificationHour: Flow<Int> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[NOTIF_HOUR_KEY] ?: 20 }

    suspend fun setNotificationHour(hour: Int) {
        ds.edit { prefs -> prefs[NOTIF_HOUR_KEY] = hour }
    }

    // ── Keys ─────────────────────────────────────────────────────────────────

    companion object {
        private val THEME_KEY          = stringPreferencesKey("theme")
        private val AUTO_ENABLED_KEY   = booleanPreferencesKey("theme_auto")
        private val DARK_AUTO_KEY      = stringPreferencesKey("theme_dark_auto")
        private val LIGHT_AUTO_KEY     = stringPreferencesKey("theme_light_auto")
        private val SNAPSHOTS_KEY      = stringPreferencesKey("snapshots")
        private val QUICK_SELECT_KEY   = stringPreferencesKey("quick_select")
        private val BACKUP_ENABLED_KEY = booleanPreferencesKey("backup_enabled")
        private val BACKUP_FREQ_KEY    = stringPreferencesKey("backup_frequency")
        private val NOTIF_ENABLED_KEY  = booleanPreferencesKey("notif_enabled")
        private val NOTIF_HOUR_KEY     = intPreferencesKey("notif_hour")
    }
}
