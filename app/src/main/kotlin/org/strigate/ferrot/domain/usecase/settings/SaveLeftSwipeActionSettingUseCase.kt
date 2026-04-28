package org.strigate.ferrot.domain.usecase.settings

import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveLeftSwipeActionSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(action: DownloadSwipeAction) {
        settingsRepository.saveLeftSwipeAction(action)
    }
}
