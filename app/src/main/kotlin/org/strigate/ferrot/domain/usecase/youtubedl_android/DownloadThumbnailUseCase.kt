package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import javax.inject.Inject

class DownloadThumbnailUseCase @Inject constructor(
    private val buildThumbnailRequestUseCase: BuildThumbnailRequestUseCase,
) {
    operator fun invoke(
        url: String,
        outputDir: File,
        videoId: String? = null,
    ): String? {
        val id = videoId ?: YoutubeDL.getInstance().getInfo(url).id ?: return null
        val youtubeDLRequest = buildThumbnailRequestUseCase(
            url = url,
            outputDir = outputDir,
            convertToJpg = true,
        )
        val youtubeDLResponse = YoutubeDL.getInstance().execute(youtubeDLRequest)
        if (youtubeDLResponse.exitCode != 0) {
            return null
        }
        val candidates = listOf(
            File(outputDir, "$id.jpg"),
            File(outputDir, "$id.jpeg"),
            File(outputDir, "$id.webp"),
            File(outputDir, "$id.png"),
        )
        return candidates
            .firstOrNull {
                it.exists() && it.length() > 0
            }
            ?.absolutePath
    }
}
