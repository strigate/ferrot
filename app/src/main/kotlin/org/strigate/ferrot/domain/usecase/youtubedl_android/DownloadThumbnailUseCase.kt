package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import org.strigate.ferrot.extensions.toSafeFileName
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

            val tempDir = File(outputDir, ".tmp_${thumbnailBaseName}")
            if (tempDir.exists() && !tempDir.deleteRecursively()) {
                return@withContext null
            }
            if (!tempDir.mkdirs()) {
                return@withContext null
            }

            val youtubeDLRequest = buildThumbnailRequestUseCase(
                url = url,
                outputDir = tempDir,
                videoId = id,
                convertToJpg = true,
            )
            val youtubeDLResponse = YoutubeDL.getInstance().execute(youtubeDLRequest)
            if (youtubeDLResponse.exitCode != 0) {
                tempDir.deleteRecursively()
                return@withContext null
            }
            val downloadedFile = tempDir
                .listFiles()
                ?.sortedBy { it.name }
                ?.firstOrNull { file ->
                    file.isFile &&
                            file.length() > 0 &&
                            file.nameWithoutExtension == thumbnailBaseName &&
                            file.extension.isNotBlank()
                }
                ?: run {
                    tempDir.deleteRecursively()
                    return@withContext null
                }

            val finalFile = File(outputDir, downloadedFile.name)
            runCatching {
                Files.move(
                    downloadedFile.toPath(),
                    finalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                tempDir.deleteRecursively()
                return@withContext null
            }
            outputDir
                .listFiles()
                ?.filter { file ->
                    file.isFile
                            && file.absolutePath != finalFile.absolutePath
                            && file.nameWithoutExtension == thumbnailBaseName
                            && file.extension.isNotBlank()
                }
                ?.forEach { file ->
                    file.delete()
                }
            tempDir.deleteRecursively()
            finalFile.absolutePath
        }
    }
}

private fun thumbnailOutputBaseName(videoId: String): String =
    "thumb_${videoId.toSafeFileName()}"
