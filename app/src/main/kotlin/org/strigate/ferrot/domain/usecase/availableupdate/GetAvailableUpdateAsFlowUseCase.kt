package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject

class GetAvailableUpdateAsFlowUseCase @Inject constructor(
    private val availableUpdateRepository: AvailableUpdateRepository,
) {
    operator fun invoke(): Flow<AvailableUpdate?> {
        return availableUpdateRepository.getAsFlow()
    }
}
