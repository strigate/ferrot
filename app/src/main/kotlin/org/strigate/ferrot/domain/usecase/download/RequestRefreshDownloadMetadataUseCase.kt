package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.RefreshDownloadMetadataWorker
import javax.inject.Inject

class RequestRefreshDownloadMetadataUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(downloadId: Long) {
        RefreshDownloadMetadataWorker.enqueueOneTimeKeep(appContext, downloadId)
    }
}
