package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.worker.DeleteAllDuplicateDownloadsWorker
import javax.inject.Inject

class ApplyAutomaticDuplicateDownloadDeletionSettingUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(automaticDuplicateDownloadDeletion: Boolean) {
        if (automaticDuplicateDownloadDeletion) {
            DeleteAllDuplicateDownloadsWorker.enqueuePeriodicKeep(appContext)
        } else {
            DeleteAllDuplicateDownloadsWorker.cancelPeriodic(appContext)
        }
    }
}
