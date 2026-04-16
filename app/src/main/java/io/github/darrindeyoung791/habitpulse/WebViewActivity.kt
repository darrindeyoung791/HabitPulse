package io.github.darrindeyoung791.habitpulse

import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.screens.WebViewScreen
import io.github.darrindeyoung791.habitpulse.ui.theme.HabitPulseTheme

class WebViewActivity : ComponentActivity() {
    companion object {
        const val EXTRA_INITIAL_URL = "extra_initial_url"
    }

    private lateinit var webView: WebView

    // Back callback for pre-Android 13 devices
    private var onBackPressedCallback: OnBackPressedCallback? = null
    // Back callback for Android 13+ devices
    private var onBackInvokedCallback: OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create WebView instance to be shared with WebViewScreen
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Register back handler at Activity level - this handles predictive back gesture properly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Use OnBackInvokedDispatcher for predictive back gesture support
            val callback = OnBackInvokedCallback {
                handleBackNavigation()
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback
            )
            onBackInvokedCallback = callback
        } else {
            // Pre-Android 13: Use OnBackPressedDispatcher
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation()
                }
            }
            onBackPressedDispatcher.addCallback(this, callback)
            onBackPressedCallback = callback
        }

        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL) ?: RouteConfig.HELP_URL

        setContent {
            HabitPulseTheme {
                WebViewScreen(
                    initialUrl = initialUrl,
                    onClose = { finishAfterTransition() },
                    externalWebView = webView,
                    onWebViewGoBack = {
                        // Callback for UI back button to trigger web navigation
                        if (webView.canGoBack()) {
                            webView.goBack()
                        }
                    }
                )
            }
        }
    }

    /**
     * Handle back navigation: go back in web history if possible, otherwise finish Activity.
     */
    private fun handleBackNavigation() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            finishAfterTransition()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::webView.isInitialized) {
                webView.stopLoading()
                webView.removeAllViews()
                webView.destroy()
            }
        } catch (_: Exception) {
        }

        // Unregister back callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
        } else {
            onBackPressedCallback?.remove()
        }
    }
}
