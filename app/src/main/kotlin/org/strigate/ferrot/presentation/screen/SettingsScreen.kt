package org.strigate.ferrot.presentation.screen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.settings.ExpandableSettingsSection
import org.strigate.ferrot.presentation.component.settings.SwitchSetting
import org.strigate.ferrot.presentation.component.settings.TextNavigateSetting
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.state.SettingsUiState
import org.strigate.ferrot.presentation.theme.FerrotTopAppBarDefaults
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.logShown()
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
                        text = stringResource(R.string.screen_title_settings),
                    )
                },
            )
        },
        content = { contentPadding ->
            Surface(
                modifier = modifier
                    .padding(contentPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (val state = uiState) {
                    is SettingsUiState.Loading -> {
                        LoadingState(
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.Center,
                        )
                    }

                    is SettingsUiState.Data -> {
                        with(state.data) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = dimens.spacingMediumAlt)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                ExpandableSettingsSection(
                                    text = stringResource(id = R.string.settings_section_general),
                                    initialExpanded = true,
                                ) {
                                    SwitchSetting(
                                        text = stringResource(id = R.string.settings_title_download_wifi_only),
                                        description = stringResource(id = R.string.settings_description_download_wifi_only),
                                        checked = downloadWifiOnly,
                                        onCheckedChange = { checked ->
                                            viewModel.setDownloadWifiOnly(checked)
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(dimens.spacingSmall))
                                    SwitchSetting(
                                        text = stringResource(id = R.string.settings_title_automatic_duplicate_deletion),
                                        description = stringResource(id = R.string.settings_description_automatic_duplicate_deletion),
                                        checked = automaticDuplicateDownloadDeletion,
                                        onCheckedChange = { checked ->
                                            viewModel.setAutomaticDuplicateDownloadDeletion(checked)
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                                TextNavigateSetting(
                                    text = stringResource(R.string.settings_navigate_title_updates),
                                ) {
                                    navController.navigate(Screen.Updates.route)
                                }
                                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                                TextNavigateSetting(
                                    text = stringResource(R.string.settings_navigate_title_about),
                                ) {
                                    navController.navigate(Screen.About.route)
                                }
                                Spacer(modifier = Modifier.height(dimens.spacingMedium))
                            }
                        }
                    }

                    is SettingsUiState.Error -> SettingsError()
                }
            }
        },
    )
}

@Composable
private fun SettingsError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier
            .fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_settings),
    )
}
