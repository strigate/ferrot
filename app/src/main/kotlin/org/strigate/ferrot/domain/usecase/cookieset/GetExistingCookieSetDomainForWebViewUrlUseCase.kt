package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.cookies.WebViewCookieDomainResolver
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class GetExistingCookieSetDomainForWebViewUrlUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
    private val webViewCookieDomainResolver: WebViewCookieDomainResolver,
) {
    suspend operator fun invoke(url: String): String? {
        val domain = webViewCookieDomainResolver(url)
        val cookieSetIds = cookieSetRepository.getCookieSetIdsByDomains(listOf(domain))
        return domain.takeIf { cookieSetIds.isNotEmpty() }
    }
}
