package io.github.darrindeyoung791.habitpulse.ui.components.webview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebviewAwareSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var webView: WebView? = null
    private var initialY = 0f

    fun setWebView(webView: WebView?) {
        this.webView = webView
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isRefreshing || ev.pointerCount > 1) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (webView == null) {
                    return false
                }
                if (webView!!.scrollY != 0 || initialY > this@WebviewAwareSwipeRefreshLayout.height / 4) {
                    return false
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }
}
