package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.app.Constants.NAME
import org.strigate.ferrot.domain.model.QualityProfile
import javax.inject.Inject

class BuildVideoDownloadRequestUseCase @Inject constructor() {
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
            addOption("--windows-filenames")

            val encoderString = "$NAME ${BuildConfig.VERSION}"
            addOption("--add-metadata")
            addOption(
                "--postprocessor-args",
                buildString {
                    append("Merger+ffmpeg:")
                    append("-metadata encoder=\"$encoderString\" ")
                    append("-metadata encoded_by=\"$encoderString\" ")
                    append("-metadata:s:v:0 encoder=\"$encoderString\" ")
                    append("-metadata:s:v:0 encoded_by=\"$encoderString\" ")
                    append("-metadata:s:a:0 encoder=\"$encoderString\" ")
                }
            )

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
