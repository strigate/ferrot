package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadvideo.DeleteDownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadVideoAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.SaveDownloadVideoUseCase
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    val saveDownloadVideoUseCase: SaveDownloadVideoUseCase,
    val getDownloadVideoAsFlowUseCase: GetDownloadVideoAsFlowUseCase,
    val deleteDownloadVideoUseCase: DeleteDownloadVideoUseCase,
)
