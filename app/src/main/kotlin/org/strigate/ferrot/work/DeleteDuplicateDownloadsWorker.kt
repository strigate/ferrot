package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase

class DeleteDuplicateDownloadsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getLong(KEY_ID, -1L)
        if (downloadId <= 0L) {
            Log.w(LOG_TAG, "Invalid downloadId")
            return@withContext Result.success()
        }

        val downloadVideo = downloadVideoUseCase
            .getDownloadVideoByDownloadIdAsFlowUseCase(downloadId)
            .first()

        val sha256 = downloadVideo?.sha256
        if (sha256.isNullOrBlank()) {
            Log.d(LOG_TAG, "No hash for downloadId=$downloadId, skipping")
            return@withContext Result.success()
        }

        val duplicateDownloadIds = downloadVideoUseCase
            .getDownloadIdsBySha256UseCase(sha256)
            .filter { it != downloadId }

        if (duplicateDownloadIds.isEmpty()) {
            Log.d(LOG_TAG, "No duplicates found for downloadId=$downloadId")
            return@withContext Result.success()
        }

        val msg = "Deleting ${duplicateDownloadIds.size} duplicate(s) for downloadId=$downloadId"
        Log.d(LOG_TAG, msg)

        duplicateDownloadIds.forEach { duplicateDownloadId ->
            runCatching {
                deleteDownloadAndRelatedCombinedUseCase(duplicateDownloadId)
            }.onFailure {
                Log.w(LOG_TAG, "Failed while deleting downloadId=$duplicateDownloadId", it)
            }
        }
        Result.success()
    }

    companion object {
        fun enqueueOneTimeReplace(context: Context, downloadId: Long, sha256: String) {
            val inputData = Data.Builder()
                .putLong(KEY_ID, downloadId)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DeleteDuplicateDownloadsWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "dedupe-${sha256}",
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }
    }
}
