package org.strigate.ferrot.domain.usecase.downloadmetadata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.di.module.IoDispatcher
import java.io.File
import javax.inject.Inject

class IsDownloadThumbnailAvailableUseCase @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(path: String?): Boolean = withContext(ioDispatcher) {
        !path.isNullOrBlank() && File(path).let { it.isFile && it.length() > 0L }
    }
}
