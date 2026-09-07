package org.strigate.ferrot.domain.usecase.state

import org.strigate.ferrot.domain.repository.StateRepository
import javax.inject.Inject

class ToggleArchivedDownloadsGridLayoutEnabledUseCase @Inject constructor(
    private val stateRepository: StateRepository,
) {
    suspend operator fun invoke() {
        stateRepository.toggleArchivedDownloadsGridLayoutEnabled()
    }
}
