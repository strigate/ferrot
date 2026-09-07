package org.strigate.ferrot.presentation.screen.getcookies

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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

@Composable
fun GetCookiesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: GetCookiesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val focusRequester = remember { FocusRequester() }
    var addressText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(DEFAULT_URL_PREFIX.toTextFieldValue())
    }
    var requestedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var pageTitle by rememberSaveable { mutableStateOf<String?>(null) }
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
        viewModel.saveCookies(
            url = targetUrl,
            cookieHeader = readWebViewCookieHeader(targetUrl),
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

    GetCookiesScreenContent(
        state = GetCookiesScreenState(
            addressText = addressText,
            requestedUrl = requestedUrl,
            titleText = titleText,
            saveUrl = saveUrl,
            overwritePrompt = overwritePrompt,
        ),
        focusRequester = focusRequester,
        onAddressTextChange = { addressText = it },
        onLoadAddress = ::loadAddress,
        onSaveCookies = { saveCookies(saveUrl) },
        onConfirmOverwrite = { prompt ->
            overwritePrompt = null
            saveCookies(
                url = prompt.url,
                confirmOverwrite = false,
            )
        },
        onDismissOverwrite = { overwritePrompt = null },
        onClose = {
            backDispatcher?.onBackPressed() ?: navController.popBackStack()
        },
        webViewContent = { webViewModifier ->
            GetCookiesWebView(
                modifier = webViewModifier,
                requestedUrl = requestedUrl,
                onCurrentUrlChanged = { url ->
                    currentUrl = url
                    addressText = url.toTextFieldValue()
                },
                onTitleChanged = { title -> pageTitle = title },
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GetCookiesScreenContent(
    state: GetCookiesScreenState,
    focusRequester: FocusRequester,
    onAddressTextChange: (TextFieldValue) -> Unit,
    onLoadAddress: () -> Unit,
    onSaveCookies: () -> Unit,
    onConfirmOverwrite: (CookieOverwritePrompt) -> Unit,
    onDismissOverwrite: () -> Unit,
    onClose: () -> Unit,
    webViewContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current

    state.overwritePrompt?.let { prompt ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_replace_cookie_set_title),
            message = stringResource(
                R.string.confirm_dialog_replace_cookie_set_description,
                prompt.domain,
            ),
            positiveButtonText = stringResource(R.string.cookies_action_replace),
            negativeButtonText = stringResource(R.string.cancel),
            onPositiveClick = { onConfirmOverwrite(prompt) },
            onNegativeClick = onDismissOverwrite,
            onDismissRequest = onDismissOverwrite,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = RefineryTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                title = {
                    Text(text = state.titleText)
                },
                actions = {
                    IconButton(
                        enabled = !state.saveUrl.isNullOrBlank(),
                        onClick = onSaveCookies,
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
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.requestedUrl == null) {
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
                        webViewContent(
                            Modifier.requiredSize(maxWidth, maxHeight),
                        )
                    }
                    AnimatedVisibility(
                        visible = state.requestedUrl == null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        exit = slideOutVertically { -it } + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = refineryDimens.spacingMediumAlt,
                                    end = refineryDimens.spacingMediumAlt,
                                    bottom = refineryDimens.spacingMediumAlt,
                                ),
                        ) {
                            CookieLoginWarning()
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = refineryDimens.spacingSmall)
                                    .focusRequester(focusRequester),
                                value = state.addressText,
                                onValueChange = onAddressTextChange,
                                label = {
                                    Text(text = stringResource(R.string.cookies_label_login_url))
                                },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = onLoadAddress) {
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
                                keyboardActions = KeyboardActions(onGo = { onLoadAddress() }),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun CookieLoginWarning(
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            modifier = Modifier.padding(refineryDimens.spacingMedium),
            style = MaterialTheme.typography.bodySmall,
            text = stringResource(R.string.cookies_webview_warning),
        )
    }
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

internal data class GetCookiesScreenState(
    val addressText: TextFieldValue,
    val requestedUrl: String?,
    val titleText: String,
    val saveUrl: String?,
    val overwritePrompt: CookieOverwritePrompt?,
)

internal data class CookieOverwritePrompt(
    val url: String,
    val domain: String,
)
