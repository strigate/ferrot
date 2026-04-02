package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.worker.DownloadWorker
import javax.inject.Inject

class StopDownloadUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(downloadId: Long) {
        DownloadWorker.cancelUnique(appContext, downloadId)
    }
}
