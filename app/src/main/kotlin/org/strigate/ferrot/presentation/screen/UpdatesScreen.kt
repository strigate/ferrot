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
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.SystemUpdate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.UpdatesEvent
import org.strigate.ferrot.presentation.state.UpdatesUiState
import org.strigate.ferrot.presentation.util.UiFormatter
import org.strigate.ferrot.presentation.viewmodel.UpdatesViewModel
import org.strigate.refinery.component.settings.StaticSettingsSection
import org.strigate.refinery.component.settings.SwitchSetting
import org.strigate.refinery.component.settings.TextSetting
import org.strigate.refinery.theme.LocalRefineryDimens
import org.strigate.refinery.theme.RefineryTopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = hiltViewModel(),
) {
    val refineryDimens = LocalRefineryDimens.current
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is UpdatesEvent.ShowToast -> {
                    context.toast(event.textRes)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = RefineryTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = { backDispatcher?.onBackPressed() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.screen_title_updates))
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
                    is UpdatesUiState.Loading -> {
                        LoadingState(
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.Center,
                        )
                    }

                    is UpdatesUiState.Data -> {
                        with(state.data) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = refineryDimens.spacingMediumAlt)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                StaticSettingsSection(
                                    icon = Icons.Outlined.SystemUpdate,
                                    title = stringResource(R.string.settings_section_app),
                                ) {
                                    SwitchSetting(
                                        text = stringResource(R.string.settings_title_automatic_app_updates),
                                        description = stringResource(R.string.settings_description_automatic_updates),
                                        checked = settings.automaticUpdates,
                                        onCheckedChange = { checked ->
                                            viewModel.setAutomaticUpdates(checked)
                                        },
                                    )
                                    TextSetting(
                                        text = stringResource(R.string.settings_title_check_for_app_updates),
                                        description = stringResource(R.string.settings_description_check_for_app_updates),
                                    ) {
                                        viewModel.checkForAvailableUpdate()
                                    }
                                    TextSetting(
                                        text = stringResource(R.string.settings_title_last_checked_for_app_updates),
                                        description = UiFormatter.formatLastCheckedTime(
                                            context,
                                            info.lastAvailableUpdateCheckMillis,
                                        ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                                StaticSettingsSection(
                                    icon = Icons.Outlined.Extension,
                                    title = stringResource(R.string.settings_section_dependencies),
                                ) {
                                    SwitchSetting(
                                        text = stringResource(R.string.settings_title_automatic_dependency_updates),
                                        description = stringResource(R.string.settings_description_automatic_dependency_updates),
                                        checked = settings.automaticDependencyUpdates,
                                        onCheckedChange = { checked ->
                                            viewModel.setAutomaticDependencyUpdates(checked)
                                        },
                                    )
                                    TextSetting(
                                        text = stringResource(R.string.settings_title_check_dependencies_now),
                                        description = stringResource(R.string.settings_description_check_dependencies_now),
                                    ) {
                                        viewModel.checkForDependencyUpdates()
                                    }
                                    TextSetting(
                                        text = stringResource(R.string.settings_title_last_checked_for_dependency_updates),
                                        description = UiFormatter.formatLastCheckedTime(
                                            context,
                                            info.lastDependencyUpdateCheckMillis,
                                        ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                            }
                        }
                    }

                    is UpdatesUiState.Error -> UpdatesError()
                }
            }
        },
    )
}

@Composable
private fun UpdatesError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier
            .fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_update_settings),
    )
}
