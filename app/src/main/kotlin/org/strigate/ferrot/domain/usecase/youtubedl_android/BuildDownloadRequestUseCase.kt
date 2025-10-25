package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import org.strigate.ferrot.domain.model.QualityProfile
import javax.inject.Inject

class BuildDownloadRequestUseCase @Inject constructor() {
    operator fun invoke(
        url: String,
        template: String,
        qualityProfile: QualityProfile,
        noProgress: Boolean,
        printFilename: Boolean = false,
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            addOption("-f", formatSelectorFor(qualityProfile))
            addOption("-o", template)
            addOption("--restrict-filenames")
            if (qualityProfile == QualityProfile.COMPAT_2160) {
                addOption("--merge-output-format", "mp4")
            }
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
            addOption("--external-downloader", "aria2c")
            addOption("--external-downloader-args", "aria2c:-x16 -k1M")
        }
    }
}

private fun formatSelectorFor(profile: QualityProfile): String = when (profile) {
    QualityProfile.MAX -> "bv*+ba/b"
    QualityProfile.CAP_2160 -> "bv*[height<=2160]+ba/b"
    QualityProfile.COMPAT_2160 -> "bv*[vcodec^=avc1][height<=2160]+ba[acodec^=mp4a]/b[ext=mp4]"
}
