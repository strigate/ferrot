package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class GetDownloadByIdUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(id: Long): Download? {
        return downloadRepository.getById(id)
    }
}
