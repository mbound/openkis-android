package org.openkis.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches data from the new dev.catastogrotte-piemonte.net export API.
 *
 * Endpoints follow the pattern: {baseUrl}/export/{entity}/csv
 * where entity is one of: cavita-naturali, sorgenti, cavita-artificiali, etc.
 */
@Singleton
class DevSiteApi @Inject constructor(private val client: OkHttpClient) {

    /**
     * Returns true if the server exposes the /export/ CSV endpoints.
     * Used to auto-detect which sync path to use.
     */
    suspend fun isCompatible(serverUrl: String): Boolean {
        val probeUrl = "${serverUrl.trimEnd('/')}/export/cavita-naturali/csv"
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(probeUrl).head().build()
                client.newCall(request).execute().use { it.isSuccessful }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fetches the CSV export for [entityPath] (e.g. "cavita-naturali") from [serverUrl].
     */
    suspend fun fetchCsv(serverUrl: String, entityPath: String): String {
        val url = "${serverUrl.trimEnd('/')}/export/$entityPath/csv"
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} fetching $url")
                response.body?.string() ?: throw IOException("Empty response from $url")
            }
        }
    }
}
