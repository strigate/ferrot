package org.strigate.ferrot.domain.usecase.cookieset

import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject

class DeleteCookieSetsWithMissingFilesUseCase @Inject constructor(
    private val cookieSetRepository: CookieSetRepository,
    private val cookieFileStore: CookieFileStore,
    private val deleteCookieSetUseCase: DeleteCookieSetUseCase,
) {
    suspend operator fun invoke() {
        cookieSetRepository
            .getAllWithDomains()
            .filter { cookieSet ->
                cookieFileStore.readCookies(cookieSet.cookieSet.id).isNullOrBlank()
            }
            .forEach { cookieSet ->
                deleteCookieSetUseCase(cookieSet.cookieSet.id)
            }
    }
}
