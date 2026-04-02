package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.worker.DeletePendingDownloadsDelayedWorker
import javax.inject.Inject

class RequestDeletePendingDownloadsDelayedUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke() {
        DeletePendingDownloadsDelayedWorker.enqueueOneTimeReplace(
            context = appContext,
        )
    }
}
