package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import javax.inject.Inject

class GetVideoInfoUseCase @Inject constructor() {
    operator fun invoke(url: String): VideoInfo {
        return YoutubeDL.getInstance().getInfo(url)
    }
}
