package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadprogress.DeleteDownloadProgressByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.GetDownloadProgressByDownloadIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadExpectedBytesUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import javax.inject.Inject

class DownloadProgressUseCase @Inject constructor(
    val getDownloadProgressByDownloadIdAsFlowUseCase: GetDownloadProgressByDownloadIdAsFlowUseCase,
    val updateDownloadExpectedBytesUseCase: UpdateDownloadExpectedBytesUseCase,
    val updateDownloadProgressUseCase: UpdateDownloadProgressUseCase,
    val deleteDownloadProgressByDownloadIdUseCase: DeleteDownloadProgressByDownloadIdUseCase,
)
