package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DeleteDownloadsWorker
import javax.inject.Inject

class RequestDeleteDownloadsUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(downloadIds: Collection<Long>) {
        if (downloadIds.isEmpty()) {
            return
        }
        DeleteDownloadsWorker.enqueueOneTimeAppend(
            downloadIds = downloadIds,
            context = appContext,
        )
    }
}
