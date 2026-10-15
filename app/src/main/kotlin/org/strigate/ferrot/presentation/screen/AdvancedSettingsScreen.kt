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
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.component.BackTopAppBar
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.model.AdvancedSettingsUiData
import org.strigate.ferrot.presentation.state.AdvancedSettingsUiState
import org.strigate.ferrot.presentation.viewmodel.AdvancedSettingsViewModel
import org.strigate.refinery.component.settings.SettingsSection
import org.strigate.refinery.component.settings.SettingsSectionDivider
import org.strigate.refinery.component.settings.SwitchSetting
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun AdvancedSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdvancedSettingsViewModel = hiltViewModel(),
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }

    AdvancedSettingsScreenContent(
        uiState = uiState,
        onBackClick = { backDispatcher?.onBackPressed() },
        onSetRemoveMediaMetadataEnabled = viewModel::setRemoveMediaMetadataEnabled,
        onSetIncludeAttributionEnabled = viewModel::setIncludeAttributionEnabled,
        modifier = modifier,
    )
}

@Composable
internal fun AdvancedSettingsScreenContent(
    uiState: AdvancedSettingsUiState,
    onBackClick: () -> Unit,
    onSetRemoveMediaMetadataEnabled: (Boolean) -> Unit,
    onSetIncludeAttributionEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopAppBar(
                title = stringResource(R.string.screen_title_advanced),
                onBackClick = onBackClick,
            )
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (uiState) {
                is AdvancedSettingsUiState.Loading -> LoadingState(
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center,
                )

                is AdvancedSettingsUiState.Error -> ErrorState(
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center,
                    text = stringResource(R.string.error_failed_to_load_settings),
                )

                is AdvancedSettingsUiState.Data -> AdvancedSettingsContent(
                    data = uiState.data,
                    onSetRemoveMediaMetadataEnabled = onSetRemoveMediaMetadataEnabled,
                    onSetIncludeAttributionEnabled = onSetIncludeAttributionEnabled,
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsContent(
    data: AdvancedSettingsUiData,
    onSetRemoveMediaMetadataEnabled: (Boolean) -> Unit,
    onSetIncludeAttributionEnabled: (Boolean) -> Unit,
) {
    val refineryDimens = LocalRefineryDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = refineryDimens.spacingMediumAlt)
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSection(
            icon = Icons.AutoMirrored.Outlined.Label,
            title = stringResource(R.string.settings_section_media_metadata),
        ) {
            SwitchSetting(
                text = stringResource(id = R.string.settings_title_remove_media_metadata),
                extraBottomPadding = if (data.removeMediaMetadataEnabled) {
                    refineryDimens.spacingXSmall
                } else {
                    0.dp
                },
                description = stringResource(id = R.string.settings_description_remove_media_metadata),
                checked = data.removeMediaMetadataEnabled,
                onCheckedChange = onSetRemoveMediaMetadataEnabled,
            )
            if (!data.removeMediaMetadataEnabled) {
                SettingsSectionDivider()
                SwitchSetting(
                    text = stringResource(id = R.string.settings_title_include_attribution),
                    extraBottomPadding = refineryDimens.spacingXSmall,
                    description = stringResource(id = R.string.settings_description_include_attribution),
                    checked = data.includeAttributionEnabled,
                    onCheckedChange = onSetIncludeAttributionEnabled,
                )
            }
        }
        Spacer(modifier = Modifier.height(refineryDimens.spacingMedium))
    }
}
