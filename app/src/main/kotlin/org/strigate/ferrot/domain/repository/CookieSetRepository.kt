package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetWithDomains

interface CookieSetRepository {
    suspend fun saveCookieSet(cookieSet: CookieSet): Long
    suspend fun saveDomains(domains: List<CookieSetDomain>)
    fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>>
    suspend fun getAllWithDomains(): List<CookieSetWithDomains>
    suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long>
    suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains?
    suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int
    suspend fun updateLastUsedAt(id: Long, lastUsedAtMillis: Long): Int
    suspend fun deleteCookieSetById(id: Long): Int
}
