package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import javax.inject.Inject

class BuildAudioDownloadRequestUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        template: String,
        noProgress: Boolean,
        outputPathFilePath: String? = null,
        printFilename: Boolean = false,
        cookieFilePath: String? = null,
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            addOption("-f", "ba/b")
            addOption("-o", template)
            addOption("--windows-filenames")
            if (!cookieFilePath.isNullOrBlank()) {
                addOption("--cookies", cookieFilePath)
            }
            if (!outputPathFilePath.isNullOrBlank()) {
                addCommands(
                    listOf(
                        "--print-to-file",
                        audioAfterMovePathTemplate(),
                        outputPathFilePath,
                    ),
                )
            }
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", "0")
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

private fun audioAfterMovePathTemplate(): String =
    "after_move:%(filepath)s"
