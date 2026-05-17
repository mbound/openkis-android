package org.openkis.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openkis.android.data.debug.DebugLogger
import org.openkis.android.data.local.dao.ServerDao
import org.openkis.android.data.local.entity.ServerEntity
import org.openkis.android.data.remote.DevSiteApi
import org.openkis.android.data.remote.DynamicBaseUrlInterceptor
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openkis_settings")

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CaveRepository,
    private val serverDao: ServerDao,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    private val devSiteApi: DevSiteApi,
    private val debugLogger: DebugLogger
) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://catastogrotte-piemonte.net"
        const val DEV_SERVER_URL = "https://dev.catastogrotte-piemonte.net"
        private val KEY_OFFLINE_MODE = stringPreferencesKey("offline_mode")
        private val KEY_SHOW_CAVES = stringPreferencesKey("show_caves")
        private val KEY_SHOW_SPRINGS = stringPreferencesKey("show_springs")
        private val KEY_SHOW_ARTIFICIALS = stringPreferencesKey("show_artificials")
        private val KEY_DEV_SOURCES = stringPreferencesKey("dev_sources_enabled")
    }

    val servers: Flow<List<ServerEntity>> = serverDao.getAll()

    val visibleServerUrls: Flow<Set<String>> = serverDao.getAll().map { servers ->
        servers.filter { it.visible }.map { it.url }.toSet()
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

    val devSourcesEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEV_SOURCES] == "true"
    }

    suspend fun ensureDefaultServer() {
        if (serverDao.count() == 0) {
            addServer(DEFAULT_SERVER_URL, "Piemonte")
        }
        // Seed the dev server hidden by default; only inserted once
        if (serverDao.getByUrl(DEV_SERVER_URL) == null) {
            serverDao.insert(ServerEntity(url = DEV_SERVER_URL, name = "Dev — Piemonte", visible = false))
        }
    }

    suspend fun addServer(url: String, name: String = "") {
        val trimmed = url.trimEnd('/')
        val label = name.ifBlank {
            try { java.net.URI(trimmed).host ?: trimmed } catch (_: Exception) { trimmed }
        }
        serverDao.insert(ServerEntity(url = trimmed, name = label))
    }

    suspend fun removeServer(url: String) {
        repository.clearByServer(url)
        serverDao.deleteByUrl(url)
    }

    suspend fun syncServer(serverUrl: String): SyncResult {
        if (serverUrl.isBlank()) return SyncResult.Error("Server URL is blank")

        dynamicBaseUrlInterceptor.baseUrl = serverUrl
        debugLogger.i("SyncManager", "Starting sync: $serverUrl")

        val server = serverDao.getByUrl(serverUrl)
        val syncCaves = server?.syncCaves != false
        val syncSprings = server?.syncSprings != false
        val syncArtificials = server?.syncArtificials != false
        debugLogger.i("SyncManager", "Sync types — caves=$syncCaves springs=$syncSprings artificials=$syncArtificials")

        return try {
            var total = 0
            // Always probe with the caves CSV to detect server type.
            // If syncCaves=true, the content is used directly (no second download).
            // If syncCaves=false, we probe but discard the content.
            val probeResult = devSiteApi.fetchCsvOrNull(serverUrl, "cavita-naturali")
            val isDevSite = probeResult != null
            debugLogger.i("SyncManager", "Routing: ${if (isDevSite) "dev-site CSV" else "legacy JSON"}")

            if (isDevSite) {
                if (syncCaves) total += repository.syncCavesFromDevSiteContent(serverUrl, probeResult!!)
                if (syncSprings) total += repository.syncSpringsFromDevSite(serverUrl)
                if (syncArtificials) total += repository.syncArtificialsFromDevSite(serverUrl)
            } else {
                if (syncCaves) total += repository.syncCaves(serverUrl)
                if (syncSprings) total += repository.syncSprings(serverUrl)
                if (syncArtificials) total += repository.syncArtificials(serverUrl)
            }

            debugLogger.i("SyncManager", "Sync complete: $total items from $serverUrl")
            serverDao.updateLastSync(serverUrl, System.currentTimeMillis())
            SyncResult.Success(total)
        } catch (e: Exception) {
            debugLogger.e("SyncManager", "Sync error for $serverUrl: ${e.message}")
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun syncAll(): SyncResult {
        val serverList = serverDao.getAll().first()
        if (serverList.isEmpty()) return SyncResult.Error("No servers configured")

        var totalCount = 0
        val errors = mutableListOf<String>()

        for (server in serverList.filter { it.visible }) {
            when (val result = syncServer(server.url)) {
                is SyncResult.Success -> totalCount += result.count
                is SyncResult.Error -> errors.add("${server.name}: ${result.message}")
            }
        }

        return if (errors.isEmpty()) {
            SyncResult.Success(totalCount)
        } else if (totalCount > 0) {
            SyncResult.Success(totalCount)
        } else {
            SyncResult.Error(errors.joinToString("; "))
        }
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

    suspend fun setDevSourcesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEV_SOURCES] = if (enabled) "true" else "false"
        }
        if (enabled) {
            if (serverDao.getByUrl(DEV_SERVER_URL) == null) {
                serverDao.insert(ServerEntity(url = DEV_SERVER_URL, name = "Dev — Piemonte", visible = true))
            } else {
                serverDao.updateVisible(DEV_SERVER_URL, true)
            }
        } else {
            serverDao.updateVisible(DEV_SERVER_URL, false)
        }
    }

    suspend fun updateServerVisible(url: String, visible: Boolean) {
        serverDao.updateVisible(url, visible)
    }

    suspend fun updateServerSyncTypes(url: String, caves: Boolean, springs: Boolean, artificials: Boolean) {
        serverDao.updateSyncTypes(url, caves, springs, artificials)
    }

    suspend fun clearCache() {
        repository.clearAll()
        serverDao.getAll().first().forEach {
            serverDao.updateLastSync(it.url, 0L)
        }
    }
}

sealed class SyncResult {
    data class Success(val count: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
