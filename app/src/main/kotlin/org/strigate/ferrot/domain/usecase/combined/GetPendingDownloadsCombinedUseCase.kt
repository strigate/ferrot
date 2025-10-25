package org.strigate.ferrot.domain.usecase.combined

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import javax.inject.Inject

class GetPendingDownloadsCombinedUseCase @Inject constructor(
    private val downloadUseCase: DownloadUseCase,
) {
    private val requeueStatuses = setOf(
        DownloadStatus.QUEUED,
        DownloadStatus.WAITING_FOR_NETWORK,
        DownloadStatus.WAITING_FOR_WIFI,
        DownloadStatus.PAUSED,
    )

    suspend operator fun invoke(): List<Download> {
        return downloadUseCase
            .getAllDownloadsUseCase()
            .filter { it.status in requeueStatuses }
    }
}
