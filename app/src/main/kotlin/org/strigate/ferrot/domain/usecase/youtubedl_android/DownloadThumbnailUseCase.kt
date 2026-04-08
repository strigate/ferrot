package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import org.strigate.ferrot.extensions.toSafeFileName
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
            val thumbnailBaseName = thumbnailOutputBaseName(id)
            outputDir
                .listFiles()
                ?.filter { file ->
                    file.isFile &&
                            file.nameWithoutExtension == thumbnailBaseName &&
                            file.extension.isNotBlank()
                }
                ?.forEach { file ->
                    if (!file.delete()) {
                        return@withContext null
                    }
                }
            val youtubeDLRequest = buildThumbnailRequestUseCase(
                url = url,
                outputDir = outputDir,
                videoId = id,
                convertToJpg = true,
            )
            val youtubeDLResponse = YoutubeDL.getInstance().execute(youtubeDLRequest)
            if (youtubeDLResponse.exitCode != 0) {
                return@withContext null
            }
            outputDir
                .listFiles()
                ?.sortedBy { it.name }
                ?.firstOrNull { file ->
                    file.isFile &&
                            file.length() > 0 &&
                            file.nameWithoutExtension == thumbnailBaseName &&
                            file.extension.isNotBlank()
                }
                ?.absolutePath
        }
    }
}

private fun thumbnailOutputBaseName(videoId: String): String =
    "thumb_${videoId.toSafeFileName()}"
