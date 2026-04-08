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
import org.strigate.ferrot.util.calculateDailyInitialDelayMillis
import java.io.File
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
        val tag = "DeleteAllOrphanDownloadFiles:"
        Log.d(LOG_TAG, "$tag Starting orphan file cleanup")

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
            Log.w(LOG_TAG, "$tag Failed to load referenced file paths", it)
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
        var failedDeleteCount = 0
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
                        } else {
                            failedDeleteCount++
                            Log.w(LOG_TAG, "$tag Failed to delete orphan file path=$canonicalPath")
                        }
                    }.onFailure {
                        failedDeleteCount++
                        Log.w(LOG_TAG, "$tag Error deleting orphan file path=$canonicalPath", it)
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

        val message = buildString {
            append("$tag Finished orphan file cleanup: ")
            append("deleted=$deletedCount failed=$failedDeleteCount")
        }
        Log.d(LOG_TAG, message)
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
            val initialDelayMillis = calculateDailyInitialDelayMillis(targetHour)
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
