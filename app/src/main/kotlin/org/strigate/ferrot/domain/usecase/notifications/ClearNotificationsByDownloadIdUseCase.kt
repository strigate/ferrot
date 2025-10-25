package org.strigate.ferrot.domain.usecase.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.util.NotificationOps
import javax.inject.Inject

class ClearNotificationsByDownloadIdUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(downloadId: Long) {
        val extras = mapOf(
            EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOAD,
            EXTRA_DOWNLOAD_ID to downloadId.toString(),
        )
        NotificationOps.clearNotificationsByExtraValue(appContext, extras)
    }
}
