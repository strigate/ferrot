package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import javax.inject.Inject

class GetVideoInfoUseCase @Inject constructor(
    private val youtubeDlRuntimeInitializer: YoutubeDlRuntimeInitializer,
) {
    suspend operator fun invoke(url: String): VideoInfo {
        youtubeDlRuntimeInitializer.initializeIfNeeded()
        return withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().getInfo(url)
        }
    }
}
