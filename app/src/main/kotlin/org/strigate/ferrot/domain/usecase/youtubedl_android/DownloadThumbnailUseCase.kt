package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import java.io.File
import javax.inject.Inject

class DownloadThumbnailUseCase @Inject constructor(
    private val buildThumbnailRequestUseCase: BuildThumbnailRequestUseCase,
    private val youtubeDlRuntimeInitializer: YoutubeDlRuntimeInitializer,
) {
    suspend operator fun invoke(
        url: String,
        outputDir: File,
        videoId: String? = null,
    ): String? {
        youtubeDlRuntimeInitializer.initializeIfNeeded()
        return withContext(Dispatchers.IO) {
            val id = videoId ?: YoutubeDL.getInstance().getInfo(url).id ?: return@withContext null
            val youtubeDLRequest = buildThumbnailRequestUseCase(
                url = url,
                outputDir = outputDir,
                convertToJpg = true,
            )
            val youtubeDLResponse = YoutubeDL.getInstance().execute(youtubeDLRequest)
            if (youtubeDLResponse.exitCode != 0) {
                return@withContext null
            }
            val sourceFile = File(outputDir, "$id.jpg")
                .takeIf { it.exists() && it.length() > 0 }
                ?: return@withContext null

            val destinationFile = File(outputDir, "thumb_${id}.jpg")
            if (sourceFile.renameTo(destinationFile)) destinationFile.absolutePath else null
        }
    }
}
