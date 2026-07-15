package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class UpdateCookieSetLastUsedAtUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
) {
    suspend operator fun invoke(
        cookieSetId: Long,
        lastUsedAtMillis: Long = System.currentTimeMillis(),
    ) {
        cookieSetRepository.updateLastUsedAt(cookieSetId, lastUsedAtMillis)
    }
}
