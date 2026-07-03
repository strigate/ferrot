package org.strigate.ferrot.presentation.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.component.ConfirmDialog
import org.strigate.ferrot.presentation.event.GetCookiesEvent
import org.strigate.ferrot.presentation.viewmodel.GetCookiesViewModel
import org.strigate.refinery.theme.LocalRefineryDimens
import org.strigate.refinery.theme.RefineryTopAppBarDefaults
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetCookiesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: GetCookiesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val refineryDimens = LocalRefineryDimens.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val focusRequester = remember { FocusRequester() }
    var addressText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(DEFAULT_URL_PREFIX.toTextFieldValue())
    }
    var requestedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var pageTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var overwritePrompt by remember { mutableStateOf<CookieOverwritePrompt?>(null) }

    val saveUrl = currentUrl ?: requestedUrl
    val titleText = pageTitle
        ?.takeIf { it.isNotBlank() }
        ?: saveUrl?.let(::titleFromUrl)
        ?: stringResource(R.string.screen_title_get_cookies)

    fun loadAddress() {
        val url = normalizeWebViewUrl(addressText.text)
        if (url == null) {
            context.toast(R.string.toast_cookie_webview_invalid_url)
            return
        }
        keyboardController?.hide()
        focusManager.clearFocus()
        addressText = url.toTextFieldValue()
        requestedUrl = url
        currentUrl = url
        pageTitle = null
    }

    fun saveCookies(url: String?, confirmOverwrite: Boolean = true) {
        val targetUrl = url ?: return
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        viewModel.saveCookies(
            url = targetUrl,
            cookieHeader = cookieManager.getCookie(targetUrl).orEmpty(),
            confirmOverwrite = confirmOverwrite,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(requestedUrl) {
        if (requestedUrl == null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is GetCookiesEvent.ShowToast -> context.toast(event.textRes)
                is GetCookiesEvent.ShowOverwriteConfirmation -> {
                    overwritePrompt = CookieOverwritePrompt(
                        url = event.url,
                        domain = event.domain,
                    )
                }

                is GetCookiesEvent.Saved -> navController.popBackStack()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

    overwritePrompt?.let { prompt ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_replace_cookie_set_title),
            message = stringResource(
                R.string.confirm_dialog_replace_cookie_set_description,
                prompt.domain,
            ),
            positiveButtonText = stringResource(R.string.cookies_action_replace),
            negativeButtonText = stringResource(R.string.cancel),
            onPositiveClick = {
                overwritePrompt = null
                saveCookies(
                    url = prompt.url,
                    confirmOverwrite = false,
                )
            },
            onNegativeClick = {
                overwritePrompt = null
            },
            onDismissRequest = {
                overwritePrompt = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = RefineryTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            backDispatcher?.onBackPressed() ?: navController.popBackStack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                title = {
                    Text(text = titleText)
                },
                actions = {
                    IconButton(
                        enabled = !saveUrl.isNullOrBlank(),
                        onClick = { saveCookies(saveUrl) },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.cookies_action_save),
                        )
                    }
                },
            )
        },
        content = { contentPadding ->
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    if (requestedUrl == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(refineryDimens.spacingMediumAlt),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                style = MaterialTheme.typography.bodyMedium,
                                text = stringResource(R.string.cookies_webview_start_description),
                            )
                        }
                    } else {
                        GetCookiesWebView(
                            modifier = Modifier
                                .requiredSize(maxWidth, maxHeight),
                            requestedUrl = requestedUrl,
                            onWebViewCreated = { webView = it },
                            onCurrentUrlChanged = { url ->
                                currentUrl = url
                                addressText = url.toTextFieldValue()
                            },
                            onTitleChanged = { title -> pageTitle = title },
                        )
                    }
                    AnimatedVisibility(
                        visible = requestedUrl == null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        exit = slideOutVertically { -it } + fadeOut(),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = refineryDimens.spacingMediumAlt,
                                    end = refineryDimens.spacingMediumAlt,
                                    bottom = refineryDimens.spacingMediumAlt,
                                )
                                .focusRequester(focusRequester),
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text(stringResource(R.string.cookies_label_login_url)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = ::loadAddress) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.cookies_action_go),
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = false,
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = { loadAddress() }),
                        )
                    }
                }
            }
        },
    )
}

private data class CookieOverwritePrompt(
    val url: String,
    val domain: String,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GetCookiesWebView(
    requestedUrl: String?,
    onWebViewCreated: (WebView) -> Unit,
    onCurrentUrlChanged: (String) -> Unit,
    onTitleChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    userAgentString =
                        WebSettings.getDefaultUserAgent(context).withoutWebViewMarker()
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onTitleChanged(view?.title)
                        if (!url.isNullOrBlank()) {
                            onCurrentUrlChanged(url)
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
                onWebViewCreated(this)
                requestedUrl?.let { url ->
                    loadRequestedUrl(url)
                }
            }
        },
        update = { view ->
            val url = requestedUrl ?: return@AndroidView
            view.loadRequestedUrl(url)
        },
    )
}

private fun String.withoutWebViewMarker(): String {
    return replace("; wv", "")
        .replace(" Version/4.0", "")
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

private fun normalizeWebViewUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        return null
    }
    val candidate = if (trimmed.contains("://")) trimmed else "$DEFAULT_URL_PREFIX$trimmed"
    return runCatching { URI(candidate) }
        .getOrNull()
        ?.takeIf { uri ->
            uri.scheme == "http" || uri.scheme == "https"
        }
        ?.takeIf { uri -> !uri.host.isNullOrBlank() }
        ?.toString()
}

private fun titleFromUrl(url: String): String {
    return runCatching { URI(url).host }
        .getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: url
}

private fun String.toTextFieldValue(): TextFieldValue {
    return TextFieldValue(
        text = this,
        selection = TextRange(length),
    )
}

private const val DEFAULT_URL_PREFIX = "https://"
