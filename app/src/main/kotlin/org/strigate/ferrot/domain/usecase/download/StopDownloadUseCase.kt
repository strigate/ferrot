package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.work.DownloadWorker
import javax.inject.Inject

class StopDownloadUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    suspend operator fun invoke(id: Long) = withContext(Dispatchers.IO) {
        DownloadWorker.cancelUnique(appContext, id)
    }
}
