package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.usecase.combined.GetResumableDownloadsCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed

class RequeueResumableDownloadsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val getResumableDownloadsCombinedUseCase: GetResumableDownloadsCombinedUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val resumableDownloads = getResumableDownloadsCombinedUseCase()
        val tag = "RequeueResumableDownloads:"
        if (resumableDownloads.isEmpty()) {
            Log.d(LOG_TAG, "$tag No resumable downloads to requeue")
            return@withContext Result.success()
        }

        Log.d(LOG_TAG, "$tag Found ${resumableDownloads.size} resumable download(s)")
        var restartedCount = 0
        var skippedTrackedCount = 0
        resumableDownloads.forEach { download ->
            if (hasTrackedDownloadWork(download.id)) {
                Log.d(LOG_TAG, "$tag Skipping downloadId=${download.id}; work already tracked")
                skippedTrackedCount++
                return@forEach
            }
            startDownloadUseCase(download.id)
            restartedCount++
            Log.d(LOG_TAG, "$tag Requeued downloadId=${download.id}")
        }
        val message = buildString {
            append(tag)
            append(" Finished requeue: ")
            append("restarted=$restartedCount ")
            append("skippedTracked=$skippedTrackedCount")
        }
        Log.d(LOG_TAG, message)
        Result.success()
    }

    private fun hasTrackedDownloadWork(downloadId: Long): Boolean {
        val workInfos = WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWork(DownloadWorker.uniqueWorkName(downloadId))
            .get()

        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }

    companion object {
        fun enqueueOneTime(context: Context) {
            val oneTimeWorkRequestBuilder =
                OneTimeWorkRequestBuilder<RequeueResumableDownloadsWorker>()
            val oneTimeWorkRequest = oneTimeWorkRequestBuilder
                .setExpeditedIfAllowed()
                .build()

            WorkManager.getInstance(context)
                .enqueue(oneTimeWorkRequest)
        }
    }
}
