package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.CookieSetDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieSetRepositoryImpl @Inject constructor(
    private val cookieSetDao: CookieSetDao,
) : CookieSetRepository {
    override suspend fun saveCookieSet(cookieSet: CookieSet): Long {
        return cookieSetDao.insertCookieSet(cookieSet.toEntity())
    }

    override suspend fun saveDomains(domains: List<CookieSetDomain>) {
        if (domains.isEmpty()) {
            return
        }
        cookieSetDao.insertDomains(domains.map { it.toEntity() })
    }

    override fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>> {
        return cookieSetDao
            .getAllWithDomainsAsFlow()
            .map { cookieSets -> cookieSets.map { it.toDomain() } }
    }

    override suspend fun getAllWithDomains(): List<CookieSetWithDomains> {
        return cookieSetDao
            .getAllWithDomains()
            .map { it.toDomain() }
    }

    override suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long> {
        if (domains.isEmpty()) {
            return emptyList()
        }
        return cookieSetDao.getCookieSetIdsByDomains(domains)
    }

    override suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains? {
        return cookieSetDao.getByIdWithDomains(id)?.toDomain()
    }

    override suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int {
        return cookieSetDao.updateCookieFilePathById(
            id = id,
            cookieFilePath = cookieFilePath,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun updateLastUsedAt(id: Long, lastUsedAtMillis: Long): Int {
        return cookieSetDao.updateLastUsedAtById(id, lastUsedAtMillis)
    }

    override suspend fun deleteCookieSetById(id: Long): Int {
        return cookieSetDao.deleteCookieSetById(id)
    }
}
