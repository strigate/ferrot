package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DeletePendingDownloadsImmediateWorker
import javax.inject.Inject

class RequestDeletePendingDownloadsImmediateUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke() {
        DeletePendingDownloadsImmediateWorker.enqueueOneTimeReplace(
            context = appContext,
        )
    }
}
