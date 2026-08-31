package org.strigate.ferrot.presentation.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.component.Copyright
import org.strigate.ferrot.presentation.event.AboutEvent
import org.strigate.ferrot.presentation.viewmodel.AboutViewModel
import org.strigate.refinery.component.settings.SettingsIconRow
import org.strigate.refinery.component.settings.SettingsIconRowItem
import org.strigate.refinery.component.settings.StaticSettingsSection
import org.strigate.refinery.component.settings.TextSetting
import org.strigate.refinery.theme.LocalRefineryDimens
import org.strigate.refinery.theme.RefineryTopAppBarDefaults

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                AboutEvent.OpenAppInfo -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    ).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                is AboutEvent.OpenUrl -> {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, event.url.toUri()),
                        )
                    } catch (_: ActivityNotFoundException) {
                        context.toast(R.string.toast_unable_to_open_link)
                    } catch (_: SecurityException) {
                        context.toast(R.string.toast_unable_to_open_link)
                    }
                }
            }
        }
    }

    AboutScreenContent(
        onBackClick = { backDispatcher?.onBackPressed() },
        onBuildClick = viewModel::onBuildClicked,
        onUrlClick = viewModel::onUrlClicked,
        onCopyText = context::copyToClipboard,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreenContent(
    onBackClick: () -> Unit,
    onBuildClick: () -> Unit,
    onUrlClick: (String) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current
    val urlStrigate = stringResource(R.string.url_strigate)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = RefineryTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.screen_title_about),
                    )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = refineryDimens.spacingMediumAlt)
                        .verticalScroll(rememberScrollState()),
                ) {
                    StaticSettingsSection(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_section_app_info),
                    ) {
                        TextSetting(
                            text = stringResource(R.string.settings_title_build),
                            description = BuildConfig.VERSION_NAME,
                            onLongClick = {
                                onCopyText(BuildConfig.VERSION_NAME)
                            },
                        ) {
                            onBuildClick()
                        }
                    }
                    Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                    StaticSettingsSection(
                        icon = Icons.Outlined.Link,
                        title = stringResource(R.string.settings_section_links),
                    ) {
                        val urlWebsite = stringResource(R.string.url_website)
                        val urlGitHub = stringResource(R.string.url_github)
                        val urlPrivacy = stringResource(R.string.url_privacy)
                        val urlLicense = stringResource(R.string.url_license)
                        TextSetting(
                            text = stringResource(R.string.settings_title_website),
                            description = stringResource(R.string.settings_description_website),
                            onLongClick = {
                                onCopyText(urlWebsite)
                            },
                        ) {
                            onUrlClick(urlWebsite)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_github),
                            description = stringResource(R.string.settings_description_github),
                            onLongClick = {
                                onCopyText(urlGitHub)
                            },
                        ) {
                            onUrlClick(urlGitHub)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_privacy),
                            description = stringResource(R.string.settings_description_privacy),
                            onLongClick = {
                                onCopyText(urlPrivacy)
                            },
                        ) {
                            onUrlClick(urlPrivacy)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_license),
                            description = stringResource(R.string.settings_description_license),
                            onLongClick = {
                                onCopyText(urlLicense)
                            },
                        ) {
                            onUrlClick(urlLicense)
                        }
                    }
                    Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                    StaticSettingsSection {
                        val urlGitHubStrigate = stringResource(R.string.url_github_strigate)
                        val urlX = stringResource(R.string.url_x)
                        SettingsIconRow(
                            items = listOf(
                                SettingsIconRowItem(
                                    painter = painterResource(R.drawable.ic_github),
                                    contentDescription = stringResource(R.string.content_description_github),
                                    onClick = {
                                        onUrlClick(urlGitHubStrigate)
                                    },
                                ),
                                SettingsIconRowItem(
                                    painter = painterResource(R.drawable.ic_x),
                                    contentDescription = stringResource(R.string.content_description_x),
                                    onClick = {
                                        onUrlClick(urlX)
                                    },
                                ),
                            ),
                        )
                    }
                    Copyright(
                        modifier = Modifier.fillMaxWidth(),
                        onLogoClick = {
                            onUrlClick(urlStrigate)
                        },
                    )
                }
            }
        },
    )
}
