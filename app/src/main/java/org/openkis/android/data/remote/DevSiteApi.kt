package org.openkis.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openkis.android.data.debug.DebugLogger
import java.io.IOException
import java.util.concurrent.TimeUnit
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
    client: OkHttpClient,
    private val logger: DebugLogger
) {
    // Dev-site CSV exports can be slow to generate; use a 5-minute read timeout.
    // newBuilder() preserves all interceptors from the shared client.
    private val client = client.newBuilder()
        .readTimeout(5, TimeUnit.MINUTES)
        .build()


    /**
     * Fetches the CSV export for [entityPath] from [serverUrl], returning null on any error.
     * Used for the first entity fetch, which doubles as a compatibility probe:
     * a non-null result means the server supports this API format.
     */
    suspend fun fetchCsvOrNull(serverUrl: String, entityPath: String): String? {
        val url = "${serverUrl.trimEnd('/')}/export/$entityPath/csv"
        logger.d("DevSiteApi", "Fetching CSV: $url")
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.w("DevSiteApi", "CSV not available: HTTP ${response.code} for $entityPath")
                        return@withContext null
                    }
                    val body = response.body?.string()
                    if (body == null) {
                        logger.w("DevSiteApi", "Empty response for $entityPath")
                        return@withContext null
                    }
                    logger.i("DevSiteApi", "CSV fetched: ${body.length} chars for $entityPath")
                    body
                }
            }
        } catch (e: Exception) {
            logger.e("DevSiteApi", "CSV fetch error for $entityPath: ${e.message}")
            null
        }
    }

    /**
     * Fetches the CSV export for [entityPath], throwing on any error.
     * Used for subsequent entity fetches once compatibility is already confirmed.
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
