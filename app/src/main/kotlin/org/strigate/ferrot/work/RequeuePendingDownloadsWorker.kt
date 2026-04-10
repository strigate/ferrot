package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.usecase.combined.GetPendingDownloadsCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.util.setExpeditedIfAllowed

class RequeuePendingDownloadsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val getPendingDownloadsCombinedUseCase: GetPendingDownloadsCombinedUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingDownloads = getPendingDownloadsCombinedUseCase()
        val tag = "RequeuePendingDownloads:"
        if (pendingDownloads.isEmpty()) {
            Log.d(LOG_TAG, "$tag No pending downloads to requeue")
            return@withContext Result.success()
        }
        Log.d(LOG_TAG, "$tag Requeuing ${pendingDownloads.size} pending download(s)")
        pendingDownloads.forEach { download ->
            startDownloadUseCase(download.id)
        }
        Log.d(LOG_TAG, "$tag Finished requeue")
        Result.success()
    }

    companion object {
        fun enqueueOneTime(context: Context) {
            val oneTimeWorkRequestBuilder =
                OneTimeWorkRequestBuilder<RequeuePendingDownloadsWorker>()
            val oneTimeWorkRequest = oneTimeWorkRequestBuilder
                .setExpeditedIfAllowed()
                .build()

            WorkManager.getInstance(context)
                .enqueue(oneTimeWorkRequest)
        }
    }
}
