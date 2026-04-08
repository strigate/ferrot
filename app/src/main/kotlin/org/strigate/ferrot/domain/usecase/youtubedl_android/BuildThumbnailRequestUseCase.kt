package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import org.strigate.ferrot.extensions.toSafeFileName
import java.io.File
import javax.inject.Inject

class BuildThumbnailRequestUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        outputDir: File,
        videoId: String,
        convertToJpg: Boolean = true,
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            val outputFilePath = File(outputDir, thumbnailOutputTemplate(videoId)).absolutePath
            addOption("-o", outputFilePath)
            addOption("--restrict-filenames")
            addOption("--skip-download")
            addOption("--write-thumbnail")
            if (convertToJpg) {
                addOption("--convert-thumbnails", "jpg")
            }
            addOption("--no-progress")
        }
    }
}

private fun thumbnailOutputBaseName(videoId: String): String =
    "thumb_${videoId.toSafeFileName()}"

private fun thumbnailOutputTemplate(videoId: String): String =
    "${thumbnailOutputBaseName(videoId)}.%(ext)s"
