package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import javax.inject.Inject

class BuildAudioDownloadRequestUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        template: String,
        noProgress: Boolean,
        printFilename: Boolean = false,
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            addOption("-f", "ba/b")
            addOption("-o", template)
            addOption("--restrict-filenames")
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", "0")
            addOption("--keep-video")
            addOption("--no-overwrites")
            addOption("--no-post-overwrites")
            if (noProgress) {
                addOption("--no-progress")
                if (printFilename) {
                    addOption("--print", "filename")
                } else {
                    addOption("--get-filename")
                }
            } else {
                addOption("--newline")
            }
        }
    }
}
