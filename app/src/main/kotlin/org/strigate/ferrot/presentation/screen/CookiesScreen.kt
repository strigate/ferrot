package org.strigate.ferrot.presentation.screen

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.BackTopAppBar
import org.strigate.ferrot.presentation.component.ConfirmDialog
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.CookiesEvent
import org.strigate.ferrot.presentation.model.CookieSetSourceUiData
import org.strigate.ferrot.presentation.model.CookieSetUiData
import org.strigate.ferrot.presentation.state.CookiesUiState
import org.strigate.ferrot.presentation.viewmodel.CookiesViewModel
import org.strigate.refinery.component.settings.StaticSettingsSection
import org.strigate.refinery.component.settings.TextSetting
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun CookiesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: CookiesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importCookieFile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CookiesEvent.ShowToast -> context.toast(event.textRes)
            }
        }
    }

    CookiesScreenContent(
        uiState = uiState,
        onBackClick = { backDispatcher?.onBackPressed() ?: navController.popBackStack() },
        onNavigateToGetCookies = { navController.navigate(Screen.GetCookies.route) },
        onImportFile = { importLauncher.launch(COOKIE_FILE_MIME_TYPES) },
        onDeleteCookieSet = { cookieSet ->
            if (cookieSet.source == CookieSetSourceUiData.WEBVIEW) {
                clearWebViewData()
            }
            viewModel.deleteCookieSet(cookieSet.id)
        },
        modifier = modifier,
    )
}

@Composable
internal fun CookiesScreenContent(
    uiState: CookiesUiState,
    onBackClick: () -> Unit,
    onNavigateToGetCookies: () -> Unit,
    onImportFile: () -> Unit,
    onDeleteCookieSet: (CookieSetUiData) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cookieSetPendingDelete by remember { mutableStateOf<CookieSetUiData?>(null) }

    cookieSetPendingDelete?.let { cookieSet ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_delete_cookie_set_title),
            message = stringResource(
                R.string.confirm_dialog_delete_cookie_set_description,
                cookieSet.name
            ),
            positiveButtonText = stringResource(R.string.notification_action_delete),
            negativeButtonText = stringResource(R.string.cancel),
            isDestructive = true,
            onPositiveClick = {
                onDeleteCookieSet(cookieSet)
                cookieSetPendingDelete = null
            },
            onNegativeClick = {
                cookieSetPendingDelete = null
            },
            onDismissRequest = {
                cookieSetPendingDelete = null
            },
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopAppBar(
                title = stringResource(R.string.screen_title_cookies),
                onBackClick = onBackClick,
            )
        },
        content = { contentPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (val state = uiState) {
                    is CookiesUiState.Loading -> {
                        LoadingState(
                            modifier = Modifier.fillMaxSize(),
                            alignment = Alignment.Center,
                        )
                    }

                    is CookiesUiState.Data -> {
                        CookiesContent(
                            cookieSets = state.cookieSets,
                            onNavigateToGetCookies = onNavigateToGetCookies,
                            onImportFile = onImportFile,
                            onRequestDelete = { cookieSetPendingDelete = it },
                        )
                    }

                    is CookiesUiState.Error -> CookiesError()
                }
            }
        },
    )
}

@Composable
private fun CookieSetSetting(
    cookieSet: CookieSetUiData,
    description: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = refineryDimens.spacingMedium,
                vertical = refineryDimens.spacingMediumAlt,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = refineryDimens.spacingMedium),
        ) {
            Text(
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                text = cookieSet.name,
            )
            Spacer(modifier = Modifier.height(refineryDimens.spacingXSmall))
            Text(
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                text = description,
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides refineryDimens.zero,
        ) {
            IconButton(
                modifier = Modifier.size(refineryDimens.iconSmall + refineryDimens.spacingXSmallAlt),
                onClick = onDelete,
            ) {
                Icon(
                    modifier = Modifier.size(refineryDimens.iconXSmall),
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_description_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CookiesError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier.fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_cookies),
    )
}

@Composable
private fun cookieSetDescription(cookieSet: CookieSetUiData): String {
    val source = when (cookieSet.source) {
        CookieSetSourceUiData.IMPORTED_FILE -> stringResource(R.string.cookies_source_imported_file)
        CookieSetSourceUiData.WEBVIEW -> stringResource(R.string.cookies_source_webview)
    }
    val domains = if (cookieSet.domains.isEmpty()) {
        stringResource(R.string.cookies_no_domains)
    } else {
        cookieSet.domains.joinToString { domain ->
            if (domain.includeSubdomains) {
                "*.${domain.domain}"
            } else {
                domain.domain
            }
        }
    }
    return "$source - $domains"
}

private val COOKIE_FILE_MIME_TYPES = arrayOf(
    "text/*",
    "application/octet-stream",
)

private fun clearWebViewData() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies {
        cookieManager.flush()
    }
    WebStorage.getInstance().deleteAllData()
}

@Composable
private fun CookiesContent(
    cookieSets: List<CookieSetUiData>,
    onNavigateToGetCookies: () -> Unit,
    onImportFile: () -> Unit,
    onRequestDelete: (CookieSetUiData) -> Unit,
) {
    val refineryDimens = LocalRefineryDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = refineryDimens.spacingMediumAlt)
            .verticalScroll(rememberScrollState()),
    ) {
        StaticSettingsSection(
            icon = Icons.Outlined.Cookie,
            title = stringResource(R.string.cookies_section_add),
        ) {
            TextSetting(
                text = stringResource(R.string.cookies_title_get_cookies),
                description = stringResource(R.string.cookies_description_get_cookies),
                onClick = onNavigateToGetCookies,
            )
            TextSetting(
                text = stringResource(R.string.cookies_title_import_file),
                description = stringResource(R.string.cookies_description_import_file),
                onClick = onImportFile,
            )
        }
        Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
        StaticSettingsSection(
            icon = Icons.Outlined.Cookie,
            title = stringResource(R.string.cookies_section_saved),
        ) {
            if (cookieSets.isEmpty()) {
                TextSetting(
                    text = stringResource(R.string.cookies_empty_title),
                    description = stringResource(R.string.cookies_empty_description),
                    enabled = false,
                )
            } else {
                cookieSets.forEach { cookieSet ->
                    CookieSetSetting(
                        cookieSet = cookieSet,
                        description = cookieSetDescription(cookieSet),
                        onDelete = { onRequestDelete(cookieSet) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(refineryDimens.spacingMedium))
    }
}
