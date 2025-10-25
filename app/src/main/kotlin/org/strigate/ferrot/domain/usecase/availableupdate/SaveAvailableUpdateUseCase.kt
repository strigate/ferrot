package org.strigate.ferrot.domain.usecase.availableupdate

import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject

class SaveAvailableUpdateUseCase @Inject constructor(
    private val availableUpdateRepository: AvailableUpdateRepository,
) {
    suspend operator fun invoke(tag: String, localFilePath: String?) {
        availableUpdateRepository.save(
            AvailableUpdate(
                tag = tag,
                localFilePath = localFilePath,
            ),
        )
    }
}
