package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.download.DeleteDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.SaveDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadCompletedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadFilePathUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStartedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusByIdUseCase
import javax.inject.Inject

class DownloadUseCase @Inject constructor(
    val saveDownloadUseCase: SaveDownloadUseCase,
    val getAllDownloadsUseCase: GetAllDownloadsUseCase,
    val getDownloadByIdUseCase: GetDownloadByIdUseCase,
    val getDownloadByIdAsFlowUseCase: GetDownloadByIdAsFlowUseCase,
    val updateDownloadCompletedAtUseCase: UpdateDownloadCompletedAtUseCase,
    val updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase,
    val updateDownloadFilePathUseCase: UpdateDownloadFilePathUseCase,
    val updateDownloadStartedAtUseCase: UpdateDownloadStartedAtUseCase,
    val updateDownloadStatusByIdUseCase: UpdateDownloadStatusByIdUseCase,
    val deleteDownloadByIdUseCase: DeleteDownloadByIdUseCase,
    val deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase,
)
