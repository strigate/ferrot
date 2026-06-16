package org.strigate.ferrot.domain.usecase.dependencyupdate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

class RequestDependencyUpdateCheckUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    operator fun invoke() {
        UpdateDependenciesWorker.enqueueOneTimeReplace(appContext)
    }
}
