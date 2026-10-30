package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearAvailableUpdateFilesAndDataUseCase @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val availableUpdateRepository: AvailableUpdateRepository,
    private val updatePathProvider: UpdatePathProvider,
) {
    suspend operator fun invoke(): Boolean = withContext(ioDispatcher) {
        val dbCleared = availableUpdateRepository.delete() == 1
        val filesCleared = runCatching {
            val updatesDir = updatePathProvider.updatesDir()
            !updatesDir.exists() || updatesDir.deleteRecursively()
        }.getOrDefault(false)

        dbCleared || filesCleared
    }
}
