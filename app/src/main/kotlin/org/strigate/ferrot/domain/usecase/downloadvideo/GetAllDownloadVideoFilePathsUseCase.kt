package org.strigate.ferrot.domain.usecase.downloadvideo

import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class GetAllDownloadVideoFilePathsUseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    suspend operator fun invoke(): List<String> {
        return downloadVideoRepository.getAllFilePaths()
    }
}
