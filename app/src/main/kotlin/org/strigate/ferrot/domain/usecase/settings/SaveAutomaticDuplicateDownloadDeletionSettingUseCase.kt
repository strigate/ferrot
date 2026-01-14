package org.strigate.ferrot.domain.usecase.settings

import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveAutomaticDuplicateDownloadDeletionSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.saveAutomaticDuplicateDownloadDeletion(enabled)
    }
}
