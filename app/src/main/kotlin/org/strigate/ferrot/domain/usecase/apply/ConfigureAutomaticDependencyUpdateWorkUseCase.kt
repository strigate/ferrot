package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

class ConfigureAutomaticDependencyUpdateWorkUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke(automaticDependencyUpdatesEnabled: Boolean) {
        if (automaticDependencyUpdatesEnabled) {
            UpdateDependenciesWorker.enqueuePeriodicKeep(appContext)
        } else {
            UpdateDependenciesWorker.cancelPeriodic(appContext)
        }
    }
}
