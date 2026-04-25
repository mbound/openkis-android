package org.openkis.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openkis.android.data.remote.DynamicBaseUrlInterceptor
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openkis_settings")

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CaveRepository,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://catastogrotte-piemonte.net"
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync")
        private val KEY_OFFLINE_MODE = stringPreferencesKey("offline_mode")
        private val KEY_SHOW_CAVES = stringPreferencesKey("show_caves")
        private val KEY_SHOW_SPRINGS = stringPreferencesKey("show_springs")
        private val KEY_SHOW_ARTIFICIALS = stringPreferencesKey("show_artificials")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    val lastSync: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC] ?: 0L
    }

    val offlineMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OFFLINE_MODE] == "true"
    }

    val showCaves: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_CAVES] != "false"
    }

    val showSprings: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_SPRINGS] != "false"
    }

    val showArtificials: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_ARTIFICIALS] != "false"
    }

    suspend fun setServerUrl(url: String) {
        val trimmed = url.trimEnd('/')
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = trimmed
        }
        dynamicBaseUrlInterceptor.baseUrl = trimmed
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OFFLINE_MODE] = if (enabled) "true" else "false"
        }
    }

    suspend fun setShowCaves(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_CAVES] = if (enabled) "true" else "false"
        }
    }

    suspend fun setShowSprings(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_SPRINGS] = if (enabled) "true" else "false"
        }
    }

    suspend fun setShowArtificials(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_ARTIFICIALS] = if (enabled) "true" else "false"
        }
    }

    suspend fun syncAll(): SyncResult {
        val url = context.dataStore.data.first()[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL
        if (url.isBlank()) return SyncResult.Error("Server URL not configured")

        dynamicBaseUrlInterceptor.baseUrl = url

        return try {
            var total = 0
            total += repository.syncCaves(url)
            total += repository.syncSprings(url)
            total += repository.syncArtificials(url)

            context.dataStore.edit { prefs ->
                prefs[KEY_LAST_SYNC] = System.currentTimeMillis()
            }
            SyncResult.Success(total)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun clearCache() {
        repository.clearAll()
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_SYNC)
        }
    }
}

sealed class SyncResult {
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
