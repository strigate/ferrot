package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class GetCookieSetByIdWithDomainsUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
) {
    suspend operator fun invoke(cookieSetId: Long): CookieSetWithDomains? {
        return cookieSetRepository.getByIdWithDomains(cookieSetId)
    }
}
