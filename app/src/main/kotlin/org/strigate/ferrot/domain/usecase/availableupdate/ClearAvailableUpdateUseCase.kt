package org.strigate.ferrot.domain.usecase.availableupdate

import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject

class ClearAvailableUpdateUseCase @Inject constructor(
    private val availableUpdateRepository: AvailableUpdateRepository,
) {
    suspend operator fun invoke(): Boolean {
        return availableUpdateRepository.delete() == 1
    }
}
