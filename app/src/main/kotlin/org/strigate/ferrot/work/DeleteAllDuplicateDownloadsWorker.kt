package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_DUPLICATE_DOWNLOADS
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DELETE_DUPLICATE_DOWNLOADS
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.util.calculateDailyInitialDelayMillis
import java.util.concurrent.TimeUnit

class DeleteAllDuplicateDownloadsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadUseCase: DownloadUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "DeleteAllDuplicateDownloads:"
        Log.d(LOG_TAG, "$tag Starting duplicate scan")

        val downloads = runCatching {
            downloadUseCase
                .getAllDownloadsUseCase()
                .filter {
                    it.status == DownloadStatus.COMPLETED
                }

        }.getOrElse {
            Log.w(LOG_TAG, "$tag Failed to load downloads", it)
            return@withContext Result.failure()
        }
        val completedDownloadIds = downloads.map { it.id }.toSet()

        var deletedCount = 0
        var failedDeleteCount = 0
        val deletedDownloadIds = mutableSetOf<Long>()
        downloads.forEach { download ->
            if (download.id in deletedDownloadIds) {
                return@forEach
            }
            val downloadMetadata = downloadMetadataUseCase
                .getDownloadMetadataByIdAsFlowUseCase(download.id)
                .first() ?: return@forEach

            val source = downloadMetadata.source ?: return@forEach
            val videoId = downloadMetadata.videoId ?: return@forEach
            val duplicateDownloadIds = downloadMetadataUseCase
                .getDownloadIdsBySourceAndVideoIdUseCase(
                    source = source,
                    videoId = videoId,
                )

            val result = deleteDuplicateCandidates(
                duplicateDownloadIds = duplicateDownloadIds,
                completedDownloadIds = completedDownloadIds,
                deletedDownloadIds = deletedDownloadIds,
                matchType = "source/videoId",
            )
            deletedCount += result.deletedCount
            failedDeleteCount += result.failedDeleteCount
        }

        downloads.forEach { download ->
            if (download.id in deletedDownloadIds) {
                return@forEach
            }
            val downloadVideo = downloadVideoUseCase
                .getDownloadVideoByDownloadIdAsFlowUseCase(download.id)
                .first() ?: return@forEach

            val sha256 = downloadVideo.sha256 ?: return@forEach
            val duplicateDownloadIds = downloadVideoUseCase
                .getDownloadIdsBySha256UseCase(sha256)

            val result = deleteDuplicateCandidates(
                duplicateDownloadIds = duplicateDownloadIds,
                completedDownloadIds = completedDownloadIds,
                deletedDownloadIds = deletedDownloadIds,
                matchType = "sha256",
            )
            deletedCount += result.deletedCount
            failedDeleteCount += result.failedDeleteCount
        }

        val message = buildString {
            append("$tag Finished duplicate scan: ")
            append("deleted=$deletedCount failed=$failedDeleteCount")
        }
        Log.d(LOG_TAG, message)
        Result.success()
    }

    private fun filterDeletionCandidateIds(
        duplicateDownloadIds: List<Long>,
        completedDownloadIds: Set<Long>,
        deletedDownloadIds: Set<Long>,
    ): List<Long> {
        return duplicateDownloadIds.filter { it in completedDownloadIds && it !in deletedDownloadIds }
    }

    private suspend fun deleteDuplicateCandidates(
        duplicateDownloadIds: List<Long>,
        completedDownloadIds: Set<Long>,
        deletedDownloadIds: MutableSet<Long>,
        matchType: String,
    ): DeleteResult {
        val deletionCandidateIds = filterDeletionCandidateIds(
            duplicateDownloadIds = duplicateDownloadIds,
            completedDownloadIds = completedDownloadIds,
            deletedDownloadIds = deletedDownloadIds,
        )
        if (deletionCandidateIds.size <= 1) {
            return DeleteResult()
        }

        val keepDownloadId = deletionCandidateIds.maxOrNull() ?: return DeleteResult()
        var deletedCount = 0
        var failedDeleteCount = 0

        deletionCandidateIds
            .filter { it != keepDownloadId }
            .forEach { deleteId ->
                runCatching {
                    val deleted = deleteDownloadAndRelatedCombinedUseCase(deleteId)
                    if (deleted) {
                        deletedDownloadIds += deleteId
                        deletedCount++
                    } else {
                        failedDeleteCount++
                        val message = buildString {
                            append("DeleteAllDuplicateDownloads: Failed to fully delete duplicate by ")
                            append("$matchType ")
                            append("downloadId=$deleteId")
                        }
                        Log.w(LOG_TAG, message)
                    }
                }.onFailure {
                    failedDeleteCount++
                    val message = buildString {
                        append("DeleteAllDuplicateDownloads: Failed to delete duplicate by ")
                        append("$matchType ")
                        append("downloadId=$deleteId")
                    }
                    Log.w(LOG_TAG, message, it)
                }
            }

        return DeleteResult(
            deletedCount = deletedCount,
            failedDeleteCount = failedDeleteCount,
        )
    }

    private data class DeleteResult(
        val deletedCount: Int = 0,
        val failedDeleteCount: Int = 0,
    )

    companion object {
        fun enqueueDebouncedReplace(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DeleteAllDuplicateDownloadsWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DELETE_DUPLICATE_DOWNLOADS,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        fun enqueuePeriodicKeep(
            context: Context,
            targetHour: Int = 3,
            flexHours: Long = 1,
        ) {
            val initialDelayMillis = calculateDailyInitialDelayMillis(targetHour)
            val periodicWorkRequest = PeriodicWorkRequestBuilder<DeleteAllDuplicateDownloadsWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = flexHours,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_DELETE_DUPLICATE_DOWNLOADS,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_DELETE_DUPLICATE_DOWNLOADS)
        }
    }
}
