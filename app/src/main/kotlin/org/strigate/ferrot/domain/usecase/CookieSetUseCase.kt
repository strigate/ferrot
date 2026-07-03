package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.cookieset.CreateCookieSetFromFileUseCase
import org.strigate.ferrot.domain.usecase.cookieset.CreateCookieSetFromWebViewUseCase
import org.strigate.ferrot.domain.usecase.cookieset.DeleteCookieSetUseCase
import org.strigate.ferrot.domain.usecase.cookieset.GetCookieSetsWithDomainsAsFlowUseCase
import org.strigate.ferrot.domain.usecase.cookieset.GetExistingCookieSetDomainForWebViewUrlUseCase
import org.strigate.ferrot.domain.usecase.cookieset.ResolveCookieSetForUrlUseCase
import org.strigate.ferrot.domain.usecase.cookieset.UpdateCookieSetLastUsedAtUseCase
import javax.inject.Inject

class CookieSetUseCase @Inject constructor(
    val createCookieSetFromFileUseCase: CreateCookieSetFromFileUseCase,
    val createCookieSetFromWebViewUseCase: CreateCookieSetFromWebViewUseCase,
    val deleteCookieSetUseCase: DeleteCookieSetUseCase,
    val getExistingCookieSetDomainForWebViewUrlUseCase: GetExistingCookieSetDomainForWebViewUrlUseCase,
    val getCookieSetsWithDomainsAsFlowUseCase: GetCookieSetsWithDomainsAsFlowUseCase,
    val resolveCookieSetForUrlUseCase: ResolveCookieSetForUrlUseCase,
    val updateCookieSetLastUsedAtUseCase: UpdateCookieSetLastUsedAtUseCase,
)
