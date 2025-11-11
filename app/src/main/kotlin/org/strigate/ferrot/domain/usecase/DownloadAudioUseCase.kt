package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadaudio.DeleteDownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.GetDownloadAudioAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.UpdateDownloadAudioFilePathUseCase
import javax.inject.Inject

class DownloadAudioUseCase @Inject constructor(
    val updateDownloadAudioFilePathUseCase: UpdateDownloadAudioFilePathUseCase,
    val getDownloadAudioAsFlowUseCase: GetDownloadAudioAsFlowUseCase,
    val deleteDownloadAudioUseCase: DeleteDownloadAudioUseCase,
)
