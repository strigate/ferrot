package org.strigate.ferrot.domain.usecase.settings

import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveAutomaticAppUpdatesEnabledSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.saveAutomaticAppUpdatesEnabled(enabled)
    }
}
