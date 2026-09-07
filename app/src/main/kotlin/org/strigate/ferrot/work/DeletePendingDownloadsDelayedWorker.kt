package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_PENDING_DOWNLOADS_DELAYED
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class DeletePendingDownloadsDelayedWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadUseCase: DownloadUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val stopDownloadUseCase: StopDownloadUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "DeletePendingDownloadsDelayed:"
        val delayMillis = inputData
            .getLong(KEY_DELAY_MILLIS, DEFAULT_DELAY_MILLIS)
            .coerceAtLeast(0L)

        if (delayMillis > 0L) {
            Log.d(LOG_TAG, "$tag Waiting ${delayMillis}ms before delete")
            delay(delayMillis.milliseconds)
        }

        val pendingDeleteIds = downloadUseCase
            .getAllDownloadsUseCase()
            .asSequence()
            .filter { it.pendingDelete }
            .map { it.id }
            .toList()

        if (pendingDeleteIds.isEmpty()) {
            Log.d(LOG_TAG, "$tag No pending deletes found after wait")
            return@withContext Result.success()
        }

        enableForeground(
            notificationText = applicationContext
                .resources
                .getQuantityString(
                    R.plurals.notification_text_deleting_downloads,
                    pendingDeleteIds.size,
                ),
            indeterminate = true,
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
        private const val KEY_DELAY_MILLIS = "key_delay_millis"
        private const val DEFAULT_DELAY_MILLIS = 5_000L

        fun enqueueOneTimeReplace(
            context: Context,
            delayMillis: Long = DEFAULT_DELAY_MILLIS,
        ) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest =
                OneTimeWorkRequestBuilder<DeletePendingDownloadsDelayedWorker>()
                    .setConstraints(constraints)
                    .setInputData(
                        workDataOf(
                            KEY_DELAY_MILLIS to delayMillis.coerceAtLeast(0L),
                        ),
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS,
                    )
                    .setExpeditedIfAllowed()
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DELETE_PENDING_DOWNLOADS_DELAYED,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }
    }
}
