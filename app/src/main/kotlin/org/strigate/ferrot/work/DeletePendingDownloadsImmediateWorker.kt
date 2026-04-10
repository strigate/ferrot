package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_PENDING_DOWNLOADS_IMMEDIATE
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed
import java.util.concurrent.TimeUnit

class DeletePendingDownloadsImmediateWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadUseCase: DownloadUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val stopDownloadUseCase: StopDownloadUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "DeletePendingDownloadsImmediate:"
        val pendingDeleteIds = downloadUseCase
            .getAllDownloadsUseCase()
            .asSequence()
            .filter { it.pendingDelete }
            .map { it.id }
            .toList()

        if (pendingDeleteIds.isEmpty()) {
            Log.d(LOG_TAG, "$tag No pending deletes found")
            return@withContext Result.success()
        }

        enableForeground(
            notificationText = applicationContext
                .resources
                .getQuantityString(
                    R.plurals.notification_text_deleting_downloads,
                    pendingDeleteIds.size,
                ),
        )
        Log.d(LOG_TAG, "$tag Starting delete for ${pendingDeleteIds.size} pending download(s)")

        var deletedCount = 0
        pendingDeleteIds.forEach { downloadId ->
            runCatching {
                stopDownloadUseCase(downloadId)
                deleteDownloadAndRelatedCombinedUseCase(downloadId)
                deletedCount++
            }.onFailure {
                Log.w(LOG_TAG, "$tag Failed to delete pending downloadId=$downloadId", it)
            }
        }
        val message = buildString {
            append("$tag Finished delete: ")
            append("deleted=$deletedCount requested=${pendingDeleteIds.size}")
        }
        Log.d(LOG_TAG, message)
        Result.success()
    }

    companion object {
        fun enqueueOneTimeReplace(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest =
                OneTimeWorkRequestBuilder<DeletePendingDownloadsImmediateWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS,
                    )
                    .setExpeditedIfAllowed()
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DELETE_PENDING_DOWNLOADS_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }
    }
}
