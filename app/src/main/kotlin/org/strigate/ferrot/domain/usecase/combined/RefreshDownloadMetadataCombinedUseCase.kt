package org.strigate.ferrot.domain.usecase.combined

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import java.io.File
import javax.inject.Inject

class RefreshDownloadMetadataCombinedUseCase @Inject constructor(
    private val downloadUseCase: DownloadUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadPathProvider: DownloadPathProvider,
    private val settingsUseCase: SettingsUseCase,
    private val cookieSetUseCase: CookieSetUseCase,
    private val cookieFileStore: CookieFileStore,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        val tag = "RefreshDownloadMetadata[$downloadId]:"
        Log.d(LOG_TAG, "$tag Start")

        val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return false
        var workerCookieFile: File? = null
        try {
            workerCookieFile = prepareCookieFile(
                cookieSetId = download.cookieSetId,
                downloadId = downloadId,
            )
            val cookieFilePath = workerCookieFile?.absolutePath
            val existingMetadata = downloadMetadataUseCase
                .getDownloadMetadataByIdAsFlowUseCase(downloadId)
                .first()

            val videoInfo = runCatching {
                youtubeDlAndroidUseCase.getVideoInfoUseCase(
                    url = download.url,
                    cookieFilePath = cookieFilePath,
                )
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
                    cookieFilePath = cookieFilePath,
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

        } finally {
            withContext(NonCancellable) {
                cookieFileStore.delete(workerCookieFile)
            }
        }
    }

    private suspend fun prepareCookieFile(cookieSetId: Long?, downloadId: Long): File? {
        val id = cookieSetId ?: return null
        val useCookies = settingsUseCase
            .getUseCookiesSettingAsFlowUseCase()
            .first()
        if (!useCookies) {
            return null
        }
        cookieSetUseCase
            .getCookieSetByIdWithDomainsUseCase(id)
            ?.cookieSet
            ?: return null
        val tempFile = cookieFileStore.copyCookiesToTemp(
            cookieSetId = id,
            name = "metadata-$downloadId-${System.nanoTime()}.txt",
        ) ?: return null

        cookieSetUseCase.updateCookieSetLastUsedAtUseCase(id)
        return tempFile
    }

    private fun thumbnailExists(path: String): Boolean {
        return File(path).exists()
    }
}
