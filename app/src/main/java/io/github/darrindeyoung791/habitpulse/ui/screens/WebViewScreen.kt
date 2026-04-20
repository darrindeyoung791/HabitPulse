package io.github.darrindeyoung791.habitpulse.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.darrindeyoung791.habitpulse.R
import io.github.darrindeyoung791.habitpulse.navigation.RouteConfig
import io.github.darrindeyoung791.habitpulse.ui.components.webview.WebViewMenuButton
import io.github.darrindeyoung791.habitpulse.ui.components.webview.WebviewAwareSwipeRefreshLayout
import kotlinx.coroutines.launch

sealed class WebViewState {
    object Loading : WebViewState()
    object Success : WebViewState()
    object Error : WebViewState()
}

sealed class SslWarningState {
    object None : SslWarningState()
    data class Show(
        val url: String,
        val primaryError: String
    ) : SslWarningState()
}

sealed class NetworkErrorState {
    object None : NetworkErrorState()
    data class Show(
        val errorCode: String,
        val errorDescription: String
    ) : NetworkErrorState()
}

private fun isExternalLink(url: String): Boolean {
    val helpBase = "darrindeyoung791.github.io/HabitPulse"
    val githubBase = "github.com/darrindeyoung791/HabitPulse"

    val urlWithoutScheme = url.removePrefix("https://").removePrefix("http://")

    return !urlWithoutScheme.startsWith(helpBase, ignoreCase = true) &&
            !urlWithoutScheme.startsWith(githubBase, ignoreCase = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    initialUrl: String = RouteConfig.HELP_URL,
    onClose: () -> Unit,
    onWebViewGoBack: (() -> Unit)? = null,
    externalWebView: WebView? = null
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    var webViewState by remember { mutableStateOf<WebViewState>(WebViewState.Loading) }
    var canGoBack by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var sslWarningState by remember { mutableStateOf<SslWarningState>(SslWarningState.None) }
    var showExternalLinkWarning by remember { mutableStateOf(false) }
    var hasShownExternalLinkWarning by remember { mutableStateOf(false) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    var networkErrorState by remember { mutableStateOf<NetworkErrorState>(NetworkErrorState.None) }

    val createdHere = externalWebView == null
    val webView = remember { externalWebView ?: WebView(context) }

    LaunchedEffect(webView) {
        // Settings are already configured in Activity, but ensure they're set if WebView is created here
        if (createdHere) {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.settings.setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val isMainFrame = request.isForMainFrame

                if (isMainFrame && !hasShownExternalLinkWarning && isExternalLink(url)) {
                    pendingExternalUrl = url
                    showExternalLinkWarning = true
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webViewState = WebViewState.Loading
                currentUrl = url
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webViewState = WebViewState.Success
                isRefreshing = false
                currentUrl = url
                canGoBack = view?.canGoBack() == true
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                // This is called for all navigation events including SPA navigation
                // Update canGoBack here for reliable state tracking
                canGoBack = view?.canGoBack() == true
                if (!isReload && url != null) {
                    currentUrl = url
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                handler?.cancel()
                val sslError = error ?: return
                val errorUrl = sslError.url
                val primaryError = when (sslError.primaryError) {
                    SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
                    SslError.SSL_EXPIRED -> "SSL_EXPIRED"
                    SslError.SSL_IDMISMATCH -> "SSL_IDMISMATCH"
                    SslError.SSL_NOTYETVALID -> "SSL_NOTYETVALID"
                    SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
                    else -> "UNKNOWN"
                }
                sslWarningState = SslWarningState.Show(
                    url = errorUrl,
                    primaryError = primaryError
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val errorCode = error?.errorCode ?: return
                val description = error?.description?.toString() ?: ""

                val isMainFrame = request?.isForMainFrame == true
                if (isMainFrame) {
                    val networkErrorCodes = setOf(
                        -2,
                        -6,
                        -8,
                        -10,
                        -16
                    )

                    if (errorCode in networkErrorCodes) {
                        webViewState = WebViewState.Error
                        networkErrorState = NetworkErrorState.Show(
                            errorCode = errorCode.toString(),
                            errorDescription = description
                        )
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                pageTitle = title
            }
        }
    }

    val swipeRefreshLayout = remember {
        WebviewAwareSwipeRefreshLayout(context).apply {
            setWebView(webView)
            setOnRefreshListener {
                isRefreshing = true
                webView.reload()
            }
        }
    }

    DisposableEffect(webView) {
        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            swipeRefreshLayout.isEnabled = scrollY == 0
        }

        onDispose {
            swipeRefreshLayout.setWebView(null)
            if (createdHere) {
                webView.stopLoading()
                webView.removeAllViews()
                webView.destroy()
            }
        }
    }

    LaunchedEffect(initialUrl) {
        webView.loadUrl(initialUrl)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle ?: currentUrl ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Back button: only show when there's web history to navigate
                        if (canGoBack) {
                            IconButton(onClick = {
                                onWebViewGoBack?.invoke()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.webview_back)
                                )
                            }
                        }

                        // Close button: always visible to exit the WebView screen
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.webview_close)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val url = webView.url ?: currentUrl ?: initialUrl
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.webview_menu_share)
                        )
                    }
                    WebViewMenuButton(
                        onRefresh = {
                            isRefreshing = true
                            webView.reload()
                        },
                        onOpenInBrowser = {
                            val url = webView.url ?: currentUrl ?: initialUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AndroidView(
                    factory = {
                        swipeRefreshLayout.addView(
                            webView,
                            android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                        swipeRefreshLayout
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (webViewState == WebViewState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (webViewState == WebViewState.Error) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.webview_loading_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        TextButton(
                            onClick = {
                                isRefreshing = true
                                webView.reload()
                            }
                        ) {
                            Text(stringResource(id = R.string.webview_retry))
                        }
                    }
                }
            }
        }
    )

    if (showExternalLinkWarning) {
        AlertDialog(
            onDismissRequest = {
                showExternalLinkWarning = false
                hasShownExternalLinkWarning = true
                pendingExternalUrl = null
            },
            title = {
                Text(text = stringResource(R.string.webview_external_link_title, appName))
            },
            text = {
                Text(text = stringResource(R.string.webview_external_link_message, appName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = pendingExternalUrl
                        showExternalLinkWarning = false
                        hasShownExternalLinkWarning = true
                        pendingExternalUrl = null
                        if (url != null) {
                            webView.loadUrl(url)
                        }
                    }
                ) {
                    Text(stringResource(R.string.webview_external_link_confirm))
                }
            }
        )
    }

    if (sslWarningState is SslWarningState.Show) {
        AlertDialog(
            onDismissRequest = {
                sslWarningState = SslWarningState.None
                onClose()
            },
            title = {
                Text(text = stringResource(R.string.webview_ssl_warning_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.webview_ssl_warning_message,
                        pageTitle ?: "",
                        (sslWarningState as SslWarningState.Show).primaryError
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val state = sslWarningState as SslWarningState.Show
                        sslWarningState = SslWarningState.None
                        webView.loadUrl(state.url)
                    }
                ) {
                    Text(stringResource(R.string.webview_ssl_warning_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sslWarningState = SslWarningState.None
                        onClose()
                    }
                ) {
                    Text(stringResource(R.string.webview_ssl_warning_cancel))
                }
            }
        )
    }

    if (networkErrorState is NetworkErrorState.Show) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = {
                networkErrorState = NetworkErrorState.None
                onWebViewGoBack?.invoke()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.webview_network_error_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.webview_network_error_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.webview_network_error_tips),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                networkErrorState = NetworkErrorState.None
                                isRefreshing = true
                                webView.reload()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.webview_network_error_refresh),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                networkErrorState = NetworkErrorState.None
                                onWebViewGoBack?.invoke()
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.webview_network_error_back),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
