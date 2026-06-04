package org.strigate.ferrot.domain.usecase.orphancleanup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DeleteAllOrphanDownloadFilesWorker
import javax.inject.Inject

class EnqueueOrphanDownloadFilesCleanupUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke() {
        DeleteAllOrphanDownloadFilesWorker.enqueuePeriodicKeep(appContext)
    }
}
