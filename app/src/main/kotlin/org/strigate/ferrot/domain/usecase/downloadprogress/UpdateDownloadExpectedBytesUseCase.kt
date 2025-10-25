package org.strigate.ferrot.domain.usecase.downloadprogress

import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import javax.inject.Inject

class UpdateDownloadExpectedBytesUseCase @Inject constructor(
    private val downloadProgressRepository: DownloadProgressRepository,
) {
    suspend operator fun invoke(id: Long, expectedBytes: Long): Boolean {
        return downloadProgressRepository.updateExpectedBytes(id, expectedBytes) >= 1
    }
}
