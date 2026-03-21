package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.download.DeleteDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeleteDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsDelayedUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsImmediateUseCase
import org.strigate.ferrot.domain.usecase.download.SaveDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadCompletedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStartedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsPendingDeleteUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsSeenUseCase
import javax.inject.Inject

class DownloadUseCase @Inject constructor(
    val saveDownloadUseCase: SaveDownloadUseCase,
    val getAllDownloadsUseCase: GetAllDownloadsUseCase,
    val getDownloadByIdUseCase: GetDownloadByIdUseCase,
    val getDownloadByIdAsFlowUseCase: GetDownloadByIdAsFlowUseCase,
    val updateDownloadCompletedAtUseCase: UpdateDownloadCompletedAtUseCase,
    val updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase,
    val updateDownloadsSeenUseCase: UpdateDownloadsSeenUseCase,
    val updateDownloadsPendingDeleteUseCase: UpdateDownloadsPendingDeleteUseCase,
    val updateDownloadStartedAtUseCase: UpdateDownloadStartedAtUseCase,
    val updateDownloadStatusUseCase: UpdateDownloadStatusUseCase,
    val requestDeleteDownloadsUseCase: RequestDeleteDownloadsUseCase,
    val requestDeletePendingDownloadsDelayedUseCase: RequestDeletePendingDownloadsDelayedUseCase,
    val requestDeletePendingDownloadsImmediateUseCase: RequestDeletePendingDownloadsImmediateUseCase,
    val deleteDownloadByIdUseCase: DeleteDownloadByIdUseCase,
    val deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase,
)
