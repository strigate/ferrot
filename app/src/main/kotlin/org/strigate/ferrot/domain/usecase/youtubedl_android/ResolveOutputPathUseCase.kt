package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import org.strigate.ferrot.domain.model.QualityProfile
import javax.inject.Inject

class ResolveOutputPathUseCase @Inject constructor(
    private val buildDownloadRequestUseCase: BuildDownloadRequestUseCase,
) {
    operator fun invoke(
        url: String,
        template: String,
        qualityProfile: QualityProfile,
    ): String {
        fun invoke(printFilename: Boolean): String {
            val youtubeDLRequest = buildDownloadRequestUseCase(
                url = url,
                template = template,
                qualityProfile = qualityProfile,
                printFilename = printFilename,
                noProgress = true,
            )
            return YoutubeDL.getInstance()
                .execute(youtubeDLRequest)
                .out
                .trim()
                .lineSequence()
                .last()
                .trim()
        }
        return runCatching { invoke(true) }.getOrElse { invoke(false) }
    }
}
