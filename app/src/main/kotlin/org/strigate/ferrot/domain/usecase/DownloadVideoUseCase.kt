package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadvideo.DeleteDownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadVideoAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.UpdateDownloadVideoFilePathUseCase
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    val updateDownloadVideoFilePathUseCase: UpdateDownloadVideoFilePathUseCase,
    val getDownloadVideoAsFlowUseCase: GetDownloadVideoAsFlowUseCase,
    val deleteDownloadVideoUseCase: DeleteDownloadVideoUseCase,
)
