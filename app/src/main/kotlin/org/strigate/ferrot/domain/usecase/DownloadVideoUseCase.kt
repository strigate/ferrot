package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadvideo.DeleteDownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetAllDownloadVideoFilePathsUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadIdsBySha256UseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadVideoByDownloadIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.SaveDownloadVideoUseCase
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    val saveDownloadVideoUseCase: SaveDownloadVideoUseCase,
    val getDownloadVideoByDownloadIdAsFlowUseCase: GetDownloadVideoByDownloadIdAsFlowUseCase,
    val getDownloadIdsBySha256UseCase: GetDownloadIdsBySha256UseCase,
    val deleteDownloadVideoUseCase: DeleteDownloadVideoUseCase,
    val getAllDownloadVideoFilePathsUseCase: GetAllDownloadVideoFilePathsUseCase,
)
