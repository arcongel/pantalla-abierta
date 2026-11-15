package co.pantallaabierta.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Reproduce la transmisión en vivo de un canal dentro de la app usando el
 * reproductor incrustado OFICIAL de YouTube, desde el canal oficial del canal de TV.
 * La app no copia ni guarda la señal: YouTube la entrega con la publicidad y las
 * reglas que el canal haya puesto.
 */
class LivePlayerActivity : Activity() {

    companion object {
        const val EXTRA_CHANNEL_ID = "youtube_channel"
        const val EXTRA_NAME = "name"
    }

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var chromeClient: WebChromeClient? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
        if (channelId.isNullOrBlank()) return finish()

        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        web.setBackgroundColor(0xFF000000.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        web.webViewClient = object : WebViewClient() {
            // Si se toca el logo de YouTube o un enlace, se abre fuera de la app.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }
        }
        val chrome = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                fullscreenView = view
                fullscreenCallback = callback
                root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                web.visibility = View.GONE
                hideSystemBars()
            }

            override fun onHideCustomView() {
                fullscreenView?.let { root.removeView(it) }
                fullscreenView = null
                fullscreenCallback?.onCustomViewHidden()
                fullscreenCallback = null
                web.visibility = View.VISIBLE
            }
        }
        chromeClient = chrome
        web.webChromeClient = chrome

        val src = "https://www.youtube.com/embed/live_stream?channel=" + Uri.encode(channelId) +
            "&autoplay=1&playsinline=1&rel=0&hl=es"
        val html = """
            <!DOCTYPE html>
            <html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              html, body { margin: 0; height: 100%; background: #000; }
              iframe { position: fixed; inset: 0; width: 100%; height: 100%; border: 0; }
            </style>
            </head><body>
            <iframe src="$src"
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowfullscreen
              referrerpolicy="strict-origin-when-cross-origin"></iframe>
            </body></html>
        """.trimIndent()
        // YouTube exige saber desde qué página se incrusta el reproductor.
        web.loadDataWithBaseURL("https://co.pantallaabierta.app/", html, "text/html", "utf-8", null)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fullscreenView != null) {
            chromeClient?.onHideCustomView()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::web.isInitialized) web.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.loadUrl("about:blank")
            web.destroy()
        }
        super.onDestroy()
    }
}
