package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import javax.inject.Inject

class BuildThumbnailRequestUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        outputDir: File,
        convertToJpg: Boolean = true,
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            addOption("-o", File(outputDir, "%(id)s.%(ext)s").absolutePath)
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
