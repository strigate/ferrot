package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDependencyUpdatesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticUpdatesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetDownloadWifiOnlySettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticDependencyUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveDownloadWifiOnlySettingUseCase
import javax.inject.Inject

class SettingsUseCase @Inject constructor(
    val getDownloadWifiOnlySettingAsFlowUseCase: GetDownloadWifiOnlySettingAsFlowUseCase,
    val saveDownloadWifiOnlySettingUseCase: SaveDownloadWifiOnlySettingUseCase,
    val getAutomaticUpdatesSettingAsFlowUseCase: GetAutomaticUpdatesSettingAsFlowUseCase,
    val saveAutomaticUpdatesSettingUseCase: SaveAutomaticUpdatesSettingUseCase,
    val getAutomaticDependencyUpdatesSettingAsFlowUseCase: GetAutomaticDependencyUpdatesSettingAsFlowUseCase,
    val saveAutomaticDependencyUpdatesSettingUseCase: SaveAutomaticDependencyUpdatesSettingUseCase,
)
