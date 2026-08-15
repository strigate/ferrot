package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DeleteAllDuplicateDownloadsWorker
import javax.inject.Inject

class ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(automaticDuplicateDownloadDeletionEnabled: Boolean) {
        if (automaticDuplicateDownloadDeletionEnabled) {
            DeleteAllDuplicateDownloadsWorker.enqueuePeriodicKeep(appContext)
        } else {
            DeleteAllDuplicateDownloadsWorker.cancelPeriodic(appContext)
        }
    }
}
