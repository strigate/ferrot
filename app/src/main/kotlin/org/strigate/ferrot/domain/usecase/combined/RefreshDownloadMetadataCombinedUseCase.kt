package org.strigate.ferrot.domain.usecase.combined

import android.util.Log
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import java.io.File
import javax.inject.Inject

class RefreshDownloadMetadataCombinedUseCase @Inject constructor(
    private val downloadUseCase: DownloadUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadPathProvider: DownloadPathProvider,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        val tag = "RefreshDownloadMetadata[$downloadId]:"
        Log.d(LOG_TAG, "$tag Start")

        val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return false
        val existingMetadata = downloadMetadataUseCase
            .getDownloadMetadataByIdAsFlowUseCase(downloadId)
            .first()

        val videoInfo = runCatching {
            youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
        }.onFailure {
            Log.w(LOG_TAG, "$tag Metadata fetch failed", it)
        }.getOrNull()

        val outputDir = downloadPathProvider.uidDir(download.uid)
        val existingThumbnailPath = existingMetadata
            ?.thumbnailFilePath
            ?.takeIf(::thumbnailExists)

        val thumbnailFilePath = runCatching {
            youtubeDlAndroidUseCase.downloadThumbnailUseCase(
                url = download.url,
                outputDir = outputDir,
                videoId = videoInfo?.id ?: existingMetadata?.videoId,
            )
        }.onFailure {
            Log.w(LOG_TAG, "$tag Thumbnail fetch failed", it)
        }.getOrNull() ?: existingThumbnailPath

        if (videoInfo == null && thumbnailFilePath == null) {
            Log.w(LOG_TAG, "$tag Nothing recovered")
            return false
        }
        val downloadMetadata = DownloadMetadata(
            downloadId = downloadId,
            videoId = videoInfo?.id ?: existingMetadata?.videoId,
            source = videoInfo?.extractorKey?.lowercase() ?: existingMetadata?.source,
            title = videoInfo?.title ?: existingMetadata?.title,
            thumbnailFilePath = thumbnailFilePath,
            durationSeconds = videoInfo
                ?.duration
                ?.takeIf { it > 0 }
                ?: existingMetadata?.durationSeconds,
        )
        downloadMetadataUseCase.saveDownloadMetadataUseCase(downloadMetadata)
        val message = buildString {
            append("$tag Complete: ")
            append("metadata=${videoInfo != null} ")
            append("thumbnail=${thumbnailFilePath != null}")
        }
        Log.d(LOG_TAG, message)
        return true
    }

    private fun thumbnailExists(path: String): Boolean {
        return File(path).exists()
    }
}
