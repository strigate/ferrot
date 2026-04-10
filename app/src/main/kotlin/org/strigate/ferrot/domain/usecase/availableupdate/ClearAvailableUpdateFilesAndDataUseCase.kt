package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearAvailableUpdateFilesAndDataUseCase @Inject constructor(
    private val availableUpdateRepository: AvailableUpdateRepository,
    private val updatePathProvider: UpdatePathProvider,
) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val dbCleared = availableUpdateRepository.delete() == 1
        val filesCleared = runCatching {
            val updatesDir = updatePathProvider.updatesDir()
            !updatesDir.exists() || updatesDir.deleteRecursively()
        }.getOrDefault(false)

        dbCleared || filesCleared
    }
}
