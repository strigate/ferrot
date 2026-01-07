package org.strigate.ferrot.domain.usecase.downloadvideo

import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class GetDownloadIdsBySha256UseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    suspend operator fun invoke(sha256: String?): List<Long> {
        if (sha256.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            downloadVideoRepository.getDownloadIdsBySha256(sha256)
        }.getOrElse {
            emptyList()
        }
    }
}
