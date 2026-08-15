package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.first
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import org.strigate.ferrot.domain.usecase.settings.GetCookiesEnabledSettingAsFlowUseCase
import java.net.URI
import java.util.Locale
import javax.inject.Inject

class ResolveCookieSetForUrlUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
    private val getCookiesEnabledSettingAsFlowUseCase: GetCookiesEnabledSettingAsFlowUseCase,
) {
    suspend operator fun invoke(url: String): CookieSetWithDomains? {
        if (!getCookiesEnabledSettingAsFlowUseCase().first()) {
            return null
        }
        val host = hostFromUrl(url) ?: return null
        val matches = cookieSetRepository
            .getAllWithDomains()
            .filter { cookieSet -> cookieSet.matches(host) }

        return matches.maxByOrNull { it.cookieSet.updatedAtMillis }
    }

    private fun CookieSetWithDomains.matches(host: String): Boolean {
        return domains.any { domain ->
            host == domain.domain
                    || (domain.includeSubdomains && host.endsWith(".${domain.domain}"))
        }
    }

    private fun hostFromUrl(url: String): String? {
        return runCatching {
            URI(url).host
        }.getOrNull()
            ?.removePrefix(".")
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
    }
}
