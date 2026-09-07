package org.strigate.ferrot.presentation.screen.getcookies

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import java.net.URI

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GetCookiesWebView(
    requestedUrl: String?,
    onCurrentUrlChanged: (String) -> Unit,
    onTitleChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTitleChanged by rememberUpdatedState(onTitleChanged)
    val currentOnCurrentUrlChanged by rememberUpdatedState(onCurrentUrlChanged)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.apply {
                    cacheMode = WebSettings.LOAD_DEFAULT
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    safeBrowsingEnabled = true
                    userAgentString = WebSettings.getDefaultUserAgent(context)
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        currentOnTitleChanged(view?.title)
                        if (!url.isNullOrBlank()) {
                            currentOnCurrentUrlChanged(url)
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val uri = request?.url ?: return false
                        if (uri.scheme != "intent") {
                            return false
                        }
                        if (!request.isForMainFrame) {
                            return true
                        }
                        val fallbackUrl = runCatching {
                            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                                .getStringExtra("browser_fallback_url")
                        }.getOrNull()?.let(::normalizeWebViewUrl)
                        if (fallbackUrl != null) {
                            view?.loadUrl(fallbackUrl)
                        }
                        return true
                    }
                }
                webChromeClient = object : WebChromeClient() {}
                requestedUrl?.let { url ->
                    loadRequestedUrl(url)
                }
            }
        },
        onRelease = { view -> view.destroy() },
        update = { view ->
            val url = requestedUrl ?: return@AndroidView
            view.loadRequestedUrl(url)
        },
    )
}

private fun WebView.loadRequestedUrl(url: String) {
    if (tag == url) {
        return
    }
    tag = url
    if (width > 0 && height > 0) {
        loadUrl(url)
    } else {
        doOnLayout { loadUrl(url) }
    }
}

internal fun normalizeWebViewUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        return null
    }
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    return runCatching { URI(candidate) }
        .getOrNull()
        ?.takeIf { uri ->
            uri.scheme == "http" || uri.scheme == "https"
        }
        ?.takeIf { uri -> !uri.host.isNullOrBlank() }
        ?.toString()
}

internal fun readWebViewCookieHeader(url: String): String {
    val cookieManager = CookieManager.getInstance()
    cookieManager.flush()
    return cookieManager.getCookie(url).orEmpty()
}
