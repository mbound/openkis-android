package org.openkis.android.data.remote

import android.content.Context
import android.net.Uri

/**
 * Persists Cloudflare clearance obtained in the experimental WebView unlock flow.
 *
 * The cookie and user agent must be replayed together because Cloudflare clearance
 * can be bound to the browser identity that solved the challenge.
 */
object CloudflareSessionStore {
    private const val PREFS = "cloudflare_clearance"

    private fun key(host: String, suffix: String) = "${host.lowercase()}_$suffix"

    fun save(context: Context, url: String, cookies: String, userAgent: String) {
        val host = Uri.parse(url).host ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(host, "cookies"), cookies)
            .putString(key(host, "ua"), userAgent)
            .apply()
    }

    fun cookies(context: Context, host: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(host, "cookies"), null)

    fun userAgent(context: Context, host: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(host, "ua"), null)

    fun clear(context: Context, host: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(host, "cookies"))
            .remove(key(host, "ua"))
            .apply()
    }
}
