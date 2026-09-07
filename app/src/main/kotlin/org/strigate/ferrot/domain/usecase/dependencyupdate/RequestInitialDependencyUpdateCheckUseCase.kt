package org.strigate.ferrot.domain.usecase.dependencyupdate

import android.content.Context
import androidx.work.await
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

class RequestInitialDependencyUpdateCheckUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    suspend operator fun invoke() {
        UpdateDependenciesWorker.enqueueOneTimeKeep(appContext).await()
    }
}
