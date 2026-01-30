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
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_ORPHAN_DOWNLOAD_FILES
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DELETE_ORPHAN_DOWNLOADS
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import java.io.File
import java.time.Duration.between
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class DeleteAllOrphanDownloadFilesWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadPathProvider: DownloadPathProvider,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG, "Starting orphan download file cleanup")

        val referencedPaths = runCatching {
            val audioPaths = downloadAudioUseCase
                .getAllDownloadAudioFilePathsUseCase()
                .mapNotNull { runCatching { File(it).canonicalPath }.getOrNull() }
            val videoPaths = downloadVideoUseCase
                .getAllDownloadVideoFilePathsUseCase()
                .mapNotNull { runCatching { File(it).canonicalPath }.getOrNull() }
            val thumbnailPaths = downloadMetadataUseCase
                .getAllDownloadThumbnailFilePathsUseCase()
                .mapNotNull { runCatching { File(it).canonicalPath }.getOrNull() }

            (audioPaths + videoPaths + thumbnailPaths).toSet()
        }.getOrElse {
            Log.w(LOG_TAG, "Failed to load referenced download file paths", it)
            return@withContext Result.failure()
        }

        val downloadRoot = downloadPathProvider.outputDir()
        if (!downloadRoot.exists()) {
            return@withContext Result.success()
        }
        val rootCanonicalPath = runCatching {
            downloadRoot.canonicalPath
        }.getOrNull() ?: return@withContext Result.success()

        var deletedCount = 0
        downloadRoot
            .walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val canonicalPath = runCatching {
                    file.canonicalPath
                }.getOrNull() ?: return@forEach

                if (!canonicalPath.startsWith(rootCanonicalPath)) {
                    return@forEach
                }
                if (canonicalPath !in referencedPaths) {
                    runCatching {
                        if (file.delete()) {
                            deletedCount++
                            Log.d(LOG_TAG, "Deleted orphan download file $canonicalPath")
                        } else {
                            Log.w(LOG_TAG, "Failed to delete orphan download file $canonicalPath")
                        }
                    }.onFailure {
                        Log.w(LOG_TAG, "Error deleting orphan download file $canonicalPath", it)
                    }
                }
            }

        downloadRoot
            .walkBottomUp()
            .filter { it.isDirectory }
            .forEach { dir ->
                if (dir.listFiles().isNullOrEmpty()) {
                    dir.delete()
                }
            }

        if (deletedCount == 0) {
            Log.d(LOG_TAG, "No orphan download files found")
        } else {
            Log.d(LOG_TAG, "Finished deleting orphan download files, deleted $deletedCount file(s)")
        }
        Result.success()
    }

    companion object {
        fun enqueueDebouncedReplace(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DeleteAllOrphanDownloadFilesWorker>()
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DELETE_ORPHAN_DOWNLOAD_FILES,
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
            val periodicWorkRequest =
                PeriodicWorkRequestBuilder<DeleteAllOrphanDownloadFilesWorker>(
                    repeatInterval = 1,
                    repeatIntervalTimeUnit = TimeUnit.DAYS,
                    flexTimeInterval = flexHours,
                    flexTimeIntervalUnit = TimeUnit.HOURS,
                )
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_DELETE_ORPHAN_DOWNLOADS,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest,
            )
        }
    }
}
