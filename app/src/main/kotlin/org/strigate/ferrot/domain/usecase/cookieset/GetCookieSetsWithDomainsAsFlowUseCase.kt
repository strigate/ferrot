package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class GetCookieSetsWithDomainsAsFlowUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
) {
    operator fun invoke(): Flow<List<CookieSetWithDomains>> {
        return cookieSetRepository.getAllWithDomainsAsFlow()
    }
}
