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
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_PENDING_DOWNLOAD_DELAYED
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed
import java.util.concurrent.TimeUnit

class DeletePendingDownloadDelayedWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadUseCase: DownloadUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val stopDownloadUseCase: StopDownloadUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong(KEY_ID, -1L)
        if (downloadId <= 0L) {
            Log.w(LOG_TAG, "No valid download ID provided for single delayed delete worker")
            return@withContext Result.success()
        }

        val delayMillis = inputData
            .getLong(KEY_DELAY_MILLIS, DEFAULT_DELAY_MILLIS)
            .coerceAtLeast(0L)

        if (delayMillis > 0L) {
            Log.d(LOG_TAG, "Single delayed delete worker waiting ${delayMillis}ms for $downloadId")
            delay(delayMillis)
        }
        val download = downloadUseCase.getDownloadByIdUseCase(downloadId)
        if (download == null) {
            Log.d(LOG_TAG, "Download already gone before single delayed delete fired: $downloadId")
            return@withContext Result.success()
        }
        if (!download.pendingDelete) {
            Log.d(LOG_TAG, "Pending delete was cleared before timeout for $downloadId")
            return@withContext Result.success()
        }

        enableForeground(
            notificationText = applicationContext
                .resources
                .getQuantityString(
                    R.plurals.notification_text_deleting_downloads,
                    1,
                ),
        )
        runCatching {
            stopDownloadUseCase(downloadId)
            deleteDownloadAndRelatedCombinedUseCase(downloadId)
            Log.d(LOG_TAG, "Single delayed delete completed for $downloadId")
        }.onFailure {
            Log.w(LOG_TAG, "Single delayed delete failed for $downloadId", it)
        }
        Result.success()
    }

    companion object {
        private const val KEY_DELAY_MILLIS = "key_delay_millis"
        private const val DEFAULT_DELAY_MILLIS = 5_000L

        fun enqueueOneTimeReplace(
            context: Context,
            downloadId: Long,
            delayMillis: Long = DEFAULT_DELAY_MILLIS,
        ) {
            if (downloadId <= 0L) {
                return
            }
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest =
                OneTimeWorkRequestBuilder<DeletePendingDownloadDelayedWorker>()
                    .setConstraints(constraints)
                    .setInputData(
                        workDataOf(
                            KEY_ID to downloadId,
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
                uniqueWorkName(downloadId),
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        private fun uniqueWorkName(downloadId: Long): String {
            return "$ONETIME_DELETE_PENDING_DOWNLOAD_DELAYED-$downloadId"
        }
    }
}
