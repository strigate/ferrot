package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.settings.GetDownloadWifiOnlySettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveDownloadWifiOnlySettingUseCase
import javax.inject.Inject

class SettingsUseCase @Inject constructor(
    val getDownloadWifiOnlySettingAsFlowUseCase: GetDownloadWifiOnlySettingAsFlowUseCase,
    val saveDownloadWifiOnlySettingUseCase: SaveDownloadWifiOnlySettingUseCase,
)
