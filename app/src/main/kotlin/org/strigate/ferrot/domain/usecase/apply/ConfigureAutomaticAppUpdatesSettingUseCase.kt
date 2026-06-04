package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import javax.inject.Inject

class ConfigureAutomaticAppUpdatesSettingUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(automaticAppUpdates: Boolean) {
        if (automaticAppUpdates) {
            DownloadAvailableUpdateWorker.enqueuePeriodicKeep(appContext)
        } else {
            DownloadAvailableUpdateWorker.cancelPeriodic(appContext)
        }
    }
}
