package org.strigate.ferrot.app.integration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DownloadWorker
import javax.inject.Inject

class DownloadWorkScheduler @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    fun enqueueOneTimeReplace(downloadId: Long, wifiOnlyDownloadsEnabled: Boolean) {
        DownloadWorker.enqueueOneTimeReplace(
            context = appContext,
            id = downloadId,
            wifiOnlyDownloadsEnabled = wifiOnlyDownloadsEnabled,
        )
    }
}
