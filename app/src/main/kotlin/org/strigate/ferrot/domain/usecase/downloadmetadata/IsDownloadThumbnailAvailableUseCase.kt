package org.strigate.ferrot.domain.usecase.downloadmetadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class IsDownloadThumbnailAvailableUseCase @Inject constructor() {
    suspend operator fun invoke(path: String?): Boolean = withContext(Dispatchers.IO) {
        !path.isNullOrBlank() && File(path).let { it.isFile && it.length() > 0L }
    }
}
