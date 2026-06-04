package org.strigate.ferrot.domain.usecase.availableupdate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import javax.inject.Inject

class RequestAppUpdateCheckUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke() {
        DownloadAvailableUpdateWorker.enqueueOneTimeReplace(appContext)
    }
}
