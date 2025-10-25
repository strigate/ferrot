package org.strigate.ferrot.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class GetDownloadWifiOnlySettingAsFlowUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return settingsRepository.getDownloadWifiOnlyAsFlow()
    }
}
