package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadaudio.DeleteDownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.GetDownloadAudioAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.SaveDownloadAudioUseCase
import javax.inject.Inject

class DownloadAudioUseCase @Inject constructor(
    val saveDownloadAudioUseCase: SaveDownloadAudioUseCase,
    val getDownloadAudioAsFlowUseCase: GetDownloadAudioAsFlowUseCase,
    val deleteDownloadAudioUseCase: DeleteDownloadAudioUseCase,
)
