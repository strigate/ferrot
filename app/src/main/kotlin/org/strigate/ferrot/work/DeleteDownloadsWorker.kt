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
import kotlinx.coroutines.withContext
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DELETE_DOWNLOADS
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed
import java.util.concurrent.TimeUnit

class DeleteDownloadsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val stopDownloadUseCase: StopDownloadUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadIds = inputData
            .getLongArray(KEY_DOWNLOAD_IDS)
            ?.toList()
            ?: emptyList()

        if (downloadIds.isEmpty()) {
            Log.w(LOG_TAG, "No download IDs provided")
            return@withContext Result.success()
        }
        enableForeground(
            notificationText = applicationContext
                .resources
                .getQuantityString(
                    R.plurals.notification_text_deleting_downloads,
                    downloadIds.size,
                ),
        )
        Log.d(LOG_TAG, "Starting delete worker for ${downloadIds.size} download(s)")
        downloadIds.forEach { downloadId ->
            runCatching {
                stopDownloadUseCase(downloadId)
                deleteDownloadAndRelatedCombinedUseCase(downloadId)
                Log.d(LOG_TAG, "Deleted downloadId=$downloadId")
            }.onFailure {
                Log.w(LOG_TAG, "Failed deleting downloadId=$downloadId", it)
            }
        }
        Log.d(LOG_TAG, "Finished deleting downloads")
        Result.success()
    }

    companion object {
        private const val KEY_DOWNLOAD_IDS = "key_download_ids"

        fun enqueueOneTimeAppend(
            context: Context,
            downloadIds: Collection<Long>,
        ) {
            if (downloadIds.isEmpty()) {
                return
            }
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresStorageNotLow(false)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DeleteDownloadsWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(KEY_DOWNLOAD_IDS to downloadIds.toLongArray()),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .setExpeditedIfAllowed()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DELETE_DOWNLOADS,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                oneTimeWorkRequest,
            )
        }
    }
}
