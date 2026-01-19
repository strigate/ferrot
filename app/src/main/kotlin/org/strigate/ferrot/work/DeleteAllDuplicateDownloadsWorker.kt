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
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_CLEANUP_DUPLICATE_DOWNLOADS
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_CLEANUP_DUPLICATE_DOWNLOADS
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import java.time.Duration.between
import java.time.ZoneId
import java.time.ZonedDateTime
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
        Log.d(LOG_TAG, "Starting scan for duplicate downloads")

        val downloads = runCatching {
            downloadUseCase
                .getAllDownloadsUseCase()
                .filter {
                    it.status == DownloadStatus.COMPLETED
                }

        }.getOrElse {
            Log.w(LOG_TAG, "Failed to load downloads", it)
            return@withContext Result.failure()
        }

        downloads.forEach { download ->
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
            if (duplicateDownloadIds.size <= 1) {
                return@forEach
            }
            val keepDownloadId = duplicateDownloadIds.maxOrNull() ?: return@forEach
            duplicateDownloadIds
                .filter { it != keepDownloadId }
                .forEach { deleteId ->
                    runCatching {
                        deleteDownloadAndRelatedCombinedUseCase(deleteId)
                        val message = "Deleted duplicate downloadId=$deleteId for $source:$videoId"
                        Log.d(LOG_TAG, message)
                    }.onFailure {
                        Log.w(LOG_TAG, "Failed deleting identity duplicate $deleteId", it)
                    }
                }
        }

        downloads.forEach { download ->
            val downloadVideo = downloadVideoUseCase
                .getDownloadVideoByDownloadIdAsFlowUseCase(download.id)
                .first() ?: return@forEach

            val sha256 = downloadVideo.sha256 ?: return@forEach
            val duplicateDownloadIds = downloadVideoUseCase.getDownloadIdsBySha256UseCase(sha256)
            if (duplicateDownloadIds.size <= 1) {
                return@forEach
            }
            val keepDownloadId = duplicateDownloadIds.maxOrNull() ?: return@forEach
            duplicateDownloadIds
                .filter { it != keepDownloadId }
                .forEach { deleteId ->
                    runCatching {
                        deleteDownloadAndRelatedCombinedUseCase(deleteId)
                        val message = "Deleted duplicate downloadId=$deleteId for sha256=$sha256"
                        Log.d(LOG_TAG, message)
                    }.onFailure {
                        Log.w(LOG_TAG, "Failed deleting sha256 duplicate $deleteId", it)
                    }
                }
        }

        Log.d(LOG_TAG, "Finished scan for duplicate downloads")
        Result.success()
    }

    companion object {
        fun enqueueDebouncedCleanup(context: Context) {
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
                ONETIME_CLEANUP_DUPLICATE_DOWNLOADS,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        fun enqueuePeriodicKeep(
            context: Context,
            targetHour: Int = 4,
            flexHours: Long = 1,
        ) {
            val zoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zoneId)

            val targetDateTime = now
                .withHour(targetHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val firstRun = if (now.isBefore(targetDateTime)) {
                targetDateTime
            } else {
                targetDateTime.plusDays(1)
            }
            val initialDelayMillis = between(now, firstRun).toMillis()
            val periodicWorkRequest = PeriodicWorkRequestBuilder<DeleteAllDuplicateDownloadsWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = flexHours,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_CLEANUP_DUPLICATE_DOWNLOADS,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_CLEANUP_DUPLICATE_DOWNLOADS)
        }
    }
}
