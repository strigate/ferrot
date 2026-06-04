package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

class ConfigureAutomaticDependencyUpdatesSettingUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(automaticDependencyUpdates: Boolean) {
        if (automaticDependencyUpdates) {
            UpdateDependenciesWorker.enqueuePeriodicKeep(appContext)
        } else {
            UpdateDependenciesWorker.cancelPeriodic(appContext)
        }
    }
}
