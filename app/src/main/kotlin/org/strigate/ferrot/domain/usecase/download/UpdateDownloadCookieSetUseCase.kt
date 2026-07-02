package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadCookieSetUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadId: Long, cookieSetId: Long?): Int {
        return downloadRepository.updateCookieSetIdById(downloadId, cookieSetId)
    }
}
