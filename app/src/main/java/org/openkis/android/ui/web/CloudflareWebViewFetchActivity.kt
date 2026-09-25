package org.openkis.android.ui.web

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.json.JSONTokener
import org.openkis.android.data.remote.CloudflareFetchBridge
import java.io.IOException

class CloudflareWebViewFetchActivity : Activity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        private const val CHUNK_SIZE = 24_000
        private const val MAX_BODY_CHARS = 25_000_000
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var requestId: String
    private lateinit var allowedHost: String
    private lateinit var statusView: TextView
    private lateinit var webView: WebView

    private var completed = false
    private var sequenceActive = false
    private var fetchIndex = 0
    private val results = linkedMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        val request = CloudflareFetchBridge.get(requestId)
        if (request == null) {
            finish()
            return
        }

        allowedHost = Uri.parse(request.baseUrl).host.orEmpty()
        if (allowedHost.isBlank()) {
            fail(IOException("Invalid browser fallback host"))
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusView = TextView(this).apply {
            text = "Opening Piemonte server verification…"
            setPadding(24, 18, 24, 18)
        }
        root.addView(
            statusView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host.orEmpty()
                    return host != allowedHost && host != "challenges.cloudflare.com"
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (!completed && !sequenceActive) {
                        inspectPage()
                    }
                }
            }

            setDownloadListener { _, _, _, _, _ ->
                if (!completed && !sequenceActive) {
                    statusView.text = "Verification passed. Preparing API session…"
                    stopLoading()
                    loadUrl(request.baseUrl + "/")
                }
            }
        }
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val cancel = Button(this).apply {
            text = "Cancel"
            setOnClickListener {
                fail(IOException("Cloudflare browser sync cancelled"))
            }
        }
        root.addView(
            cancel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        statusView.text = "Checking Cloudflare verification…"
        webView.loadUrl(request.urls.first())
    }

    private fun inspectPage() {
        if (completed || sequenceActive) return
        val script = """
            (function() {
              var body = document.body ? document.body.innerText : '';
              return JSON.stringify({
                title: document.title || '',
                href: location.href || '',
                prefix: body.slice(0, 320)
              });
            })()
        """.trimIndent()

        webView.evaluateJavascript(script) { raw ->
            if (completed || sequenceActive) return@evaluateJavascript

            val decoded = decodeEvalString(raw)
            val info = runCatching { JSONObject(decoded) }.getOrNull()
            val title = info?.optString("title").orEmpty()
            val href = info?.optString("href").orEmpty()
            val prefix = info?.optString("prefix").orEmpty()

            val challenge =
                title.contains("Just a moment", ignoreCase = true) ||
                    prefix.contains("security verification", ignoreCase = true) ||
                    prefix.contains("verify you are human", ignoreCase = true)

            if (challenge) {
                statusView.text =
                    "Cloudflare verification required. Complete any check shown below; sync will continue automatically."
                handler.postDelayed({ inspectPage() }, 1_000L)
                return@evaluateJavascript
            }

            val currentHost = runCatching { Uri.parse(href).host }.getOrNull()
            if (currentHost != allowedHost) {
                statusView.text = "Returning to Piemonte server…"
                webView.loadUrl(CloudflareFetchBridge.get(requestId)?.baseUrl + "/")
                return@evaluateJavascript
            }

            sequenceActive = true
            statusView.text = "Verification passed. Fetching API data in browser session…"
            fetchNext()
        }
    }

    private fun fetchNext() {
        if (completed) return
        val request = CloudflareFetchBridge.get(requestId)
            ?: return fail(IOException("Browser sync request expired"))

        if (fetchIndex >= request.urls.size) {
            completed = true
            statusView.text = "Browser sync complete."
            CloudflareFetchBridge.complete(requestId, results)
            finish()
            return
        }

        val target = request.urls[fetchIndex]
        statusView.text = "Fetching ${fetchIndex + 1} of ${request.urls.size}…"

        val quotedTarget = JSONObject.quote(target)
        val script = """
            (function() {
              window.__openkisBody = null;
              window.__openkisMeta = null;
              fetch($quotedTarget, {credentials: 'include', cache: 'no-store'})
                .then(async function(r) {
                  var text = await r.text();
                  window.__openkisBody = text;
                  window.__openkisMeta = JSON.stringify({
                    ok: true,
                    status: r.status,
                    len: text.length,
                    mitigated: r.headers.get('cf-mitigated') || '',
                    prefix: text.slice(0, 220)
                  });
                })
                .catch(function(e) {
                  window.__openkisMeta = JSON.stringify({
                    ok: false,
                    error: String(e)
                  });
                });
              return 'started';
            })()
        """.trimIndent()

        webView.evaluateJavascript(script) {
            handler.postDelayed({ pollFetchMeta(target) }, 250L)
        }
    }

    private fun pollFetchMeta(target: String) {
        if (completed || !sequenceActive) return

        webView.evaluateJavascript(
            "(function(){ return window.__openkisMeta || ''; })()"
        ) { raw ->
            if (completed || !sequenceActive) return@evaluateJavascript

            val decoded = decodeEvalString(raw)
            if (decoded.isBlank()) {
                handler.postDelayed({ pollFetchMeta(target) }, 250L)
                return@evaluateJavascript
            }

            val meta = runCatching { JSONObject(decoded) }.getOrNull()
                ?: return@evaluateJavascript fail(IOException("Invalid WebView fetch response"))

            if (!meta.optBoolean("ok", false)) {
                fail(IOException("WebView fetch failed: " + meta.optString("error")))
                return@evaluateJavascript
            }

            val status = meta.optInt("status", 0)
            val length = meta.optInt("len", -1)
            val mitigated = meta.optString("mitigated")
            val prefix = meta.optString("prefix")

            val challenged =
                status == 403 &&
                    (
                        mitigated.equals("challenge", ignoreCase = true) ||
                            prefix.contains("Just a moment", ignoreCase = true)
                        )

            if (challenged) {
                sequenceActive = false
                statusView.text =
                    "Cloudflare challenged this API endpoint. Complete verification below; it will retry automatically."
                webView.loadUrl(target)
                return@evaluateJavascript
            }

            if (status !in 200..299) {
                fail(IOException("HTTP $status fetching $target in WebView"))
                return@evaluateJavascript
            }

            if (length < 0 || length > MAX_BODY_CHARS) {
                fail(IOException("Unexpected API response size: $length characters"))
                return@evaluateJavascript
            }

            readBodyChunks(target, length, 0, StringBuilder(length.coerceAtLeast(0)))
        }
    }

    private fun readBodyChunks(
        target: String,
        length: Int,
        offset: Int,
        builder: StringBuilder
    ) {
        if (completed) return

        if (offset >= length) {
            results[target] = builder.toString()
            fetchIndex += 1
            webView.evaluateJavascript(
                "window.__openkisBody = null; window.__openkisMeta = null;"
            ) {
                fetchNext()
            }
            return
        }

        val end = minOf(offset + CHUNK_SIZE, length)
        val script =
            "(function(){ return (window.__openkisBody || '').substring($offset, $end); })()"

        webView.evaluateJavascript(script) { raw ->
            if (completed) return@evaluateJavascript
            builder.append(decodeEvalString(raw))
            readBodyChunks(target, length, end, builder)
        }
    }

    private fun decodeEvalString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return try {
            when (val value = JSONTokener(raw).nextValue()) {
                is String -> value
                else -> value?.toString().orEmpty()
            }
        } catch (_: Exception) {
            raw
        }
    }

    private fun fail(error: Throwable) {
        if (completed) return
        completed = true
        CloudflareFetchBridge.fail(requestId, error)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        fail(IOException("Cloudflare browser sync cancelled"))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        if (!completed && ::requestId.isInitialized) {
            completed = true
            CloudflareFetchBridge.fail(
                requestId,
                IOException("Cloudflare browser sync window closed")
            )
        }
        super.onDestroy()
    }
}
