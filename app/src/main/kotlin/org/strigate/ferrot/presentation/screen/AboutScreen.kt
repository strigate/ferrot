package org.strigate.ferrot.presentation.screen

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
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.presentation.component.Copyright
import org.strigate.ferrot.presentation.component.settings.StaticSettingsSection
import org.strigate.ferrot.presentation.component.settings.TextSetting
import org.strigate.ferrot.presentation.event.AboutEvent
import org.strigate.ferrot.presentation.theme.FerrotTopAppBarDefaults
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.viewmodel.AboutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
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
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, event.url.toUri())
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = FerrotTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            backDispatcher?.onBackPressed()
                        },
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
                        .padding(horizontal = dimens.spacingMediumAlt)
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
                                context.copyToClipboard(BuildConfig.VERSION_NAME)
                            },
                        ) {
                            viewModel.onBuildClicked()
                        }
                    }
                    Spacer(modifier = Modifier.height(dimens.spacingSmall))
                    StaticSettingsSection {
                        val urlWebsite = stringResource(R.string.url_website)
                        val urlPrivacy = stringResource(R.string.url_privacy)
                        val urlLicense = stringResource(R.string.url_license)
                        TextSetting(
                            text = stringResource(R.string.settings_title_website),
                            description = stringResource(R.string.settings_description_website),
                        ) {
                            viewModel.onUrlClicked(urlWebsite)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_privacy),
                            description = stringResource(R.string.settings_description_privacy),
                        ) {
                            viewModel.onUrlClicked(urlPrivacy)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_license),
                            description = stringResource(R.string.settings_description_license),
                        ) {
                            viewModel.onUrlClicked(urlLicense)
                        }
                    }
                    Copyright(
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        },
    )
}
