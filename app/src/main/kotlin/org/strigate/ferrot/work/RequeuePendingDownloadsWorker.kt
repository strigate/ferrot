package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.usecase.combined.GetPendingDownloadsCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase

@HiltWorker
class RequeuePendingDownloadsWorker @AssistedInject constructor(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val getPendingDownloadsCombinedUseCase: GetPendingDownloadsCombinedUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingDownloads = getPendingDownloadsCombinedUseCase()
        pendingDownloads.forEach { download ->
            Log.d(LOG_TAG, "Re-enqueuing download ${download.id} (${download.status})")
            startDownloadUseCase(download.id)
        }
        Result.success()
    }

    companion object {
        fun enqueueOneItem(context: Context) {
            val oneTimeWorkRequestBuilder =
                OneTimeWorkRequestBuilder<RequeuePendingDownloadsWorker>()
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequestBuilder.build())
        }
    }
}
