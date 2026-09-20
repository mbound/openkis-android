package org.openkis.android.ui.settings

import android.app.Activity
import android.graphics.Color
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
import org.openkis.android.data.remote.CloudflareSessionStore

/**
 * Experimental visible WebView used to obtain Cloudflare browser clearance.
 *
 * The resulting cookies and WebView user agent are stored locally and replayed
 * by CloudflareClearanceInterceptor on subsequent API requests.
 */
class CloudflareUnlockActivity : Activity() {

    companion object {
        const val EXTRA_URL = "url"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var targetUrl: String
    private var captured = false

    private val cookiePoll = object : Runnable {
        override fun run() {
            captureClearanceIfAvailable()
            if (!captured) handler.postDelayed(this, 750)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (targetUrl.isBlank()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        status = TextView(this).apply {
            text = "Complete the Cloudflare check below. This test build will capture the clearance cookie locally."
            setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(24, 24, 24, 16)
        }
        root.addView(
            status,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val done = Button(this).apply {
            text = "Done"
            setOnClickListener {
                captureClearanceIfAvailable()
                finish()
            }
        }
        root.addView(
            done,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        webView = WebView(this)
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                status.text = "Opening Cloudflare challenge…"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                captureClearanceIfAvailable()
                if (!captured) {
                    status.text = "Waiting for Cloudflare clearance. If a check is shown, complete it here."
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        webView.loadUrl(targetUrl)
        handler.post(cookiePoll)
    }

    private fun captureClearanceIfAvailable() {
        if (captured) return
        val cookies = CookieManager.getInstance().getCookie(targetUrl).orEmpty()
        if (!cookies.contains("cf_clearance=")) return

        CloudflareSessionStore.save(
            context = this,
            url = targetUrl,
            cookies = cookies,
            userAgent = webView.settings.userAgentString
        )
        CookieManager.getInstance().flush()
        captured = true
        status.text = "Cloudflare clearance captured. Tap Done, then retry sync."
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
