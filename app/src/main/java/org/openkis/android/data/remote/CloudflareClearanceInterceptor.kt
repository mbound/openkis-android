package org.openkis.android.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import org.openkis.android.data.debug.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Experimental client-side workaround for Cloudflare browser challenges.
 *
 * If the user has completed the WebView unlock flow for a Piemonte host,
 * replay the captured cookies and the exact WebView user agent on API requests.
 */
@Singleton
class CloudflareClearanceInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: DebugLogger
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host

        if (!isPiemonteHost(host)) {
            return chain.proceed(original)
        }

        val cookies = CloudflareSessionStore.cookies(context, host)
        val userAgent = CloudflareSessionStore.userAgent(context, host)

        val request = if (!cookies.isNullOrBlank() && !userAgent.isNullOrBlank()) {
            logger.d("Cloudflare", "Applying saved clearance for $host")
            original.newBuilder()
                .header("Cookie", cookies)
                .header("User-Agent", userAgent)
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)
        if (response.code == 403 && response.header("cf-mitigated") == "challenge") {
            logger.w(
                "Cloudflare",
                "Browser challenge returned for $host; run Experimental Cloudflare Unlock in Settings"
            )
        }
        return response
    }

    private fun isPiemonteHost(host: String): Boolean =
        host == "catastogrotte-piemonte.net" ||
            host.endsWith(".catastogrotte-piemonte.net")
}
