package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.cookies.CookieHeaderFileBuilder
import org.strigate.ferrot.cookies.ParsedCookieDomain
import org.strigate.ferrot.cookies.WebViewCookieDomainResolver
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class CreateCookieSetFromWebViewUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
    private val webViewCookieDomainResolver: WebViewCookieDomainResolver,
    private val cookieHeaderFileBuilder: CookieHeaderFileBuilder,
    private val cookieFileStore: CookieFileStore,
    private val deleteCookieSetUseCase: DeleteCookieSetUseCase,
) {
    suspend operator fun invoke(
        url: String,
        rawCookieHeader: String,
    ): CookieSetWithDomains {
        val domain = webViewCookieDomainResolver(url)
        val domains = listOf(
            ParsedCookieDomain(
                domain = domain,
                includeSubdomains = true,
            )
        )
        val cookieFileContent = cookieHeaderFileBuilder.build(domains, rawCookieHeader)
        val cookieSetId = cookieSetRepository.saveCookieSet(
            CookieSet(
                name = domain,
                source = CookieSetSource.WEBVIEW,
                cookieFilePath = "",
            )
        )

        return runCatching {
            val cookieFile = cookieFileStore.writeCookies(cookieSetId, cookieFileContent)
            cookieSetRepository.updateCookieFilePath(cookieSetId, cookieFile.absolutePath)
            cookieSetRepository.saveDomains(
                domains.map { parsedDomain ->
                    CookieSetDomain(
                        cookieSetId = cookieSetId,
                        domain = parsedDomain.domain,
                        includeSubdomains = parsedDomain.includeSubdomains,
                    )
                }
            )
            deleteCookieSetsWithMatchingDomains(cookieSetId, domains)
            requireNotNull(cookieSetRepository.getByIdWithDomains(cookieSetId))
        }.getOrElse { throwable ->
            deleteCookieSetUseCase(cookieSetId)
            throw throwable
        }
    }

    private suspend fun deleteCookieSetsWithMatchingDomains(
        currentCookieSetId: Long,
        domains: List<ParsedCookieDomain>,
    ) {
        cookieSetRepository
            .getCookieSetIdsByDomains(domains.map { it.domain })
            .filter { cookieSetId -> cookieSetId != currentCookieSetId }
            .forEach { cookieSetId ->
                deleteCookieSetUseCase(cookieSetId)
            }
    }
}
