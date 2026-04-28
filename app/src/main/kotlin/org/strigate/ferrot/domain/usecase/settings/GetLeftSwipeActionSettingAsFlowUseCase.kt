package org.strigate.ferrot.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject

class GetLeftSwipeActionSettingAsFlowUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<DownloadSwipeAction> {
        return settingsRepository.getLeftSwipeActionAsFlow()
    }
}
