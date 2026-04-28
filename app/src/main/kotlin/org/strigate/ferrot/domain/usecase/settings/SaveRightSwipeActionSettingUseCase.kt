package org.strigate.ferrot.domain.usecase.settings

import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveRightSwipeActionSettingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(action: DownloadSwipeAction) {
        settingsRepository.saveRightSwipeAction(action)
    }
}
