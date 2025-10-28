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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.component.settings.StaticSettingsSection
import org.strigate.ferrot.presentation.component.settings.SwitchSetting
import org.strigate.ferrot.presentation.component.settings.TextSetting
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.state.UpdatesUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.viewmodel.UpdatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = hiltViewModel(),
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
            ) {
                when (val state = uiState) {
                    is UpdatesUiState.Loading -> LoadingState()
                    is UpdatesUiState.Error -> UpdatesError()
                    is UpdatesUiState.Data -> {
                        with(state.data) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = dimens.spacingMediumAlt)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                StaticSettingsSection(
                                    text = stringResource(R.string.settings_section_app),
                                ) {
                                    SwitchSetting(
                                        text = stringResource(R.string.settings_title_automatic_updates),
                                        description = stringResource(R.string.settings_description_automatic_updates),
                                        checked = automaticUpdates,
                                        onCheckedChange = { checked ->
                                            viewModel.setAutomaticUpdates(checked)
                                        },
                                    )
                                    TextSetting(
                                        text = stringResource(R.string.settings_title_check_now),
                                        description = stringResource(R.string.settings_description_check_now),
                                    ) {
                                        viewModel.checkNow()
                                    }
                                }
                                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                                StaticSettingsSection(
                                    text = stringResource(R.string.settings_section_dependencies),
                                ) {
                                    SwitchSetting(
                                        text = stringResource(R.string.settings_title_automatic_dependency_updates),
                                        description = stringResource(R.string.settings_description_automatic_dependency_updates),
                                        checked = automaticDependencyUpdates,
                                        onCheckedChange = { checked ->
                                            viewModel.setAutomaticDependencyUpdates(checked)
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                            }
                        }
                    }
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
        modifier = modifier,
        text = stringResource(R.string.error_failed_to_load_update_settings),
    )
}
