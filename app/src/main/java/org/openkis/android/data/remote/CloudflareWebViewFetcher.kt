package org.openkis.android.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.openkis.android.data.debug.DebugLogger
import org.openkis.android.ui.web.CloudflareWebViewFetchActivity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal data class CloudflareFetchRequest(
    val id: String,
    val baseUrl: String,
    val urls: List<String>,
    val deferred: CompletableDeferred<Map<String, String>>
)

internal object CloudflareFetchBridge {
    private val requests = ConcurrentHashMap<String, CloudflareFetchRequest>()

    fun register(request: CloudflareFetchRequest) {
        requests[request.id] = request
    }

    fun get(id: String): CloudflareFetchRequest? = requests[id]

    fun complete(id: String, result: Map<String, String>) {
        requests[id]?.deferred?.complete(result)
    }

    fun fail(id: String, error: Throwable) {
        requests[id]?.deferred?.completeExceptionally(error)
    }

    fun remove(id: String) {
        requests.remove(id)
    }
}

@Singleton
class CloudflareWebViewFetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DebugLogger
) {
    companion object {
        private val SUPPORTED_HOSTS = setOf(
            "catastogrotte-piemonte.net",
            "dev.catastogrotte-piemonte.net"
        )
        private const val TIMEOUT_MS = 5 * 60 * 1000L
    }

    private val mutex = Mutex()

    fun supports(baseUrl: String): Boolean {
        val uri = runCatching { Uri.parse(baseUrl) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host in SUPPORTED_HOSTS
    }

    suspend fun fetchAll(baseUrl: String, urls: List<String>): Map<String, String> =
        mutex.withLock {
            require(supports(baseUrl)) { "Unsupported Cloudflare WebView host: $baseUrl" }
            require(urls.isNotEmpty()) { "No URLs requested" }

            val baseHost = Uri.parse(baseUrl).host
            urls.forEach { url ->
                val uri = Uri.parse(url)
                require(uri.scheme == "https" && uri.host == baseHost) {
                    "WebView fetch URL must stay on $baseHost"
                }
            }

            val id = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<Map<String, String>>()
            CloudflareFetchBridge.register(
                CloudflareFetchRequest(
                    id = id,
                    baseUrl = baseUrl.trimEnd('/'),
                    urls = urls,
                    deferred = deferred
                )
            )

            logger.w(
                "CloudflareWebView",
                "Launching browser-session fallback for ${urls.size} endpoint(s) on $baseHost"
            )

            val intent = Intent(context, CloudflareWebViewFetchActivity::class.java).apply {
                putExtra(CloudflareWebViewFetchActivity.EXTRA_REQUEST_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            try {
                withTimeout(TIMEOUT_MS) { deferred.await() }
            } finally {
                CloudflareFetchBridge.remove(id)
            }
        }
}
