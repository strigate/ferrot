package org.strigate.ferrot.presentation.screen

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.Screen
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
import org.strigate.refinery.theme.RefineryTopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: CookiesViewModel = hiltViewModel(),
) {
    val refineryDimens = LocalRefineryDimens.current
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val uiState by viewModel.uiState.collectAsState()

    var cookieSetPendingDelete by remember { mutableStateOf<CookieSetUiData?>(null) }
    var cookiePreview by remember { mutableStateOf<CookiePreview?>(null) }

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
                is CookiesEvent.ShowCookieText -> {
                    cookiePreview = CookiePreview(
                        title = event.title,
                        text = event.text,
                    )
                }
            }
        }
    }

    cookieSetPendingDelete?.let { cookieSet ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_delete_cookie_set_title),
            message = stringResource(
                R.string.confirm_dialog_delete_cookie_set_description,
                cookieSet.name
            ),
            positiveButtonText = stringResource(R.string.notification_action_delete),
            negativeButtonText = stringResource(R.string.cancel),
            onPositiveClick = {
                viewModel.deleteCookieSet(cookieSet.id)
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
    cookiePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { cookiePreview = null },
            title = {
                Text(text = preview.title)
            },
            text = {
                SelectionContainer {
                    Text(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        text = preview.text,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { cookiePreview = null }) {
                    Text(text = stringResource(R.string.close))
                }
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.screen_title_cookies))
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
                when (val state = uiState) {
                    is CookiesUiState.Loading -> {
                        LoadingState(
                            modifier = Modifier.fillMaxSize(),
                            alignment = Alignment.Center,
                        )
                    }

                    is CookiesUiState.Data -> {
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
                                    onClick = { navController.navigate(Screen.GetCookies.route) },
                                )
                                TextSetting(
                                    text = stringResource(R.string.cookies_title_import_file),
                                    description = stringResource(R.string.cookies_description_import_file),
                                    onClick = { importLauncher.launch(COOKIE_FILE_MIME_TYPES) },
                                )
                            }
                            Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                            StaticSettingsSection(
                                icon = Icons.Outlined.Cookie,
                                title = stringResource(R.string.cookies_section_saved),
                            ) {
                                if (state.cookieSets.isEmpty()) {
                                    TextSetting(
                                        text = stringResource(R.string.cookies_empty_title),
                                        description = stringResource(R.string.cookies_empty_description),
                                        enabled = false,
                                    )
                                } else {
                                    state.cookieSets.forEach { cookieSet ->
                                        CookieSetSetting(
                                            cookieSet = cookieSet,
                                            description = cookieSetDescription(cookieSet),
                                            onClick = {
                                                viewModel.showCookieText(
                                                    cookieSetId = cookieSet.id,
                                                    title = cookieSet.name,
                                                )
                                            },
                                            onDelete = { cookieSetPendingDelete = cookieSet },
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(refineryDimens.spacingMedium))
                        }
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

private data class CookiePreview(
    val title: String,
    val text: String,
)
