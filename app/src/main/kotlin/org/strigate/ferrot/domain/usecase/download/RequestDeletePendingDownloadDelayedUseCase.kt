package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.worker.DeletePendingDownloadDelayedWorker
import javax.inject.Inject

class RequestDeletePendingDownloadDelayedUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(downloadId: Long) {
        DeletePendingDownloadDelayedWorker.enqueueOneTimeReplace(
            context = appContext,
            downloadId = downloadId,
        )
    }
}
