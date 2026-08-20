package io.github.darrindeyoung791.habitpulse.utils

import android.content.Context
import android.webkit.WebSettings

object WebViewUserAgent {
    private const val FALLBACK_CHROME_VERSION = "109.0.0.0"

    /**
     * Returns a privacy-reduced User-Agent in the Chrome 109+ UA Reduction format:
     * device model is replaced with "K", Android version is frozen at "Android 10",
     * and the Chrome version follows the system WebView's actual version.
     */
    fun reduced(context: Context): String {
        val defaultUa = WebSettings.getDefaultUserAgent(context)
        val chromeVersion = CHROME_VERSION_REGEX.find(defaultUa)?.groupValues?.get(1)
            ?: FALLBACK_CHROME_VERSION
        val mobileToken = if (MOBILE_TOKEN_REGEX.containsMatchIn(defaultUa)) " Mobile" else ""
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chromeVersion$mobileToken Safari/537.36"
    }

    private val CHROME_VERSION_REGEX = Regex("Chrome/(\\d+\\.\\d+\\.\\d+\\.\\d+)")
    private val MOBILE_TOKEN_REGEX = Regex("\\bMobile\\b")
}