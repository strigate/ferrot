package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.youtubedl_android.DownloadThumbnailUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.DownloadWithProgressUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.GetVideoInfoUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.ResolveOutputPathUseCase
import javax.inject.Inject

class YoutubeDlAndroidUseCase @Inject constructor(
    val downloadWithProgressUseCase: DownloadWithProgressUseCase,
    val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    val resolveOutputPathUseCase: ResolveOutputPathUseCase,
    val getVideoInfoUseCase: GetVideoInfoUseCase,
)
