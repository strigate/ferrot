package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.repository.StateRepository
import javax.inject.Inject

class GetLastDependencyUpdateCheckMillisUseCase @Inject constructor(
    private val stateRepository: StateRepository,
) {
    operator fun invoke(): Flow<Long> {
        return stateRepository.getLastDependencyUpdateCheckMillisAsFlow()
    }
}
