package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_REFRESH_DOWNLOAD_METADATA
import org.strigate.ferrot.domain.usecase.combined.RefreshDownloadMetadataCombinedUseCase

class RefreshDownloadMetadataWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val refreshDownloadMetadataCombinedUseCase: RefreshDownloadMetadataCombinedUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_ID, -1L)
        val tag = "RefreshDownloadMetadata[$downloadId]:"

        if (downloadId <= 0L) {
            Log.w(LOG_TAG, "$tag Invalid download ID")
            return Result.failure()
        }
        val refreshed = runCatching {
            refreshDownloadMetadataCombinedUseCase(downloadId)
        }.onFailure {
            Log.w(LOG_TAG, "$tag Worker failed", it)
        }.getOrDefault(false)

        return if (refreshed) {
            Log.d(LOG_TAG, "$tag Complete")
            Result.success()
        } else {
            Log.w(LOG_TAG, "$tag No metadata refreshed")
            Result.failure()
        }
    }

    companion object {
        fun enqueueOneTimeKeep(context: Context, downloadId: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putLong(KEY_ID, downloadId)
                .build()

            val request = OneTimeWorkRequestBuilder<RefreshDownloadMetadataWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(downloadId),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun uniqueWorkName(downloadId: Long): String {
            return "$ONETIME_REFRESH_DOWNLOAD_METADATA-$downloadId"
        }
    }
}
