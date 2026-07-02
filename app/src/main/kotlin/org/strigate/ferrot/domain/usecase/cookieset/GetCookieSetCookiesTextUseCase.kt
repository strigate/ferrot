package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class GetCookieSetCookiesTextUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
    private val cookieFileStore: CookieFileStore,
) {
    suspend operator fun invoke(cookieSetId: Long): String? {
        cookieSetRepository.getByIdWithDomains(cookieSetId) ?: return null
        return cookieFileStore.readCookies(cookieSetId)
    }
}
