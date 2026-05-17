package org.openkis.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openkis.android.data.debug.DebugLogger
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
class DevSiteApi @Inject constructor(
    private val client: OkHttpClient,
    private val logger: DebugLogger
) {

    /**
     * Returns true if the server exposes the /export/ CSV endpoints.
     * Uses a GET request (not HEAD) because some servers return 405 for HEAD.
     */
    suspend fun isCompatible(serverUrl: String): Boolean {
        val probeUrl = "${serverUrl.trimEnd('/')}/export/cavita-naturali/csv"
        logger.d("DevSiteApi", "Probing: $probeUrl")
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(probeUrl).get().build()
                client.newCall(request).execute().use { response ->
                    val ok = response.isSuccessful
                    logger.i("DevSiteApi", "Probe result: HTTP ${response.code} → compatible=$ok")
                    ok
                }
            }
        } catch (e: Exception) {
            logger.e("DevSiteApi", "Probe failed: ${e.message}")
            false
        }
    }

    /**
     * Fetches the CSV export for [entityPath] (e.g. "cavita-naturali") from [serverUrl].
     */
    suspend fun fetchCsv(serverUrl: String, entityPath: String): String {
        val url = "${serverUrl.trimEnd('/')}/export/$entityPath/csv"
        logger.d("DevSiteApi", "Fetching CSV: $url")
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.e("DevSiteApi", "CSV fetch failed: HTTP ${response.code} for $url")
                    throw IOException("HTTP ${response.code} fetching $url")
                }
                val body = response.body?.string() ?: throw IOException("Empty response from $url")
                logger.i("DevSiteApi", "CSV fetched: ${body.length} chars for $entityPath")
                body
            }
        }
    }
}
