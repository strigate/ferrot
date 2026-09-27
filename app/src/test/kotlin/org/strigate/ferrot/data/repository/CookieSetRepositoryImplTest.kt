package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.CookieSetDao
import org.strigate.ferrot.data.local.entity.CookieSetDomainEntity
import org.strigate.ferrot.data.local.entity.CookieSetEntity
import org.strigate.ferrot.data.local.entity.CookieSetWithDomainsEntity
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource

class CookieSetRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var dao: CookieSetDao

    private lateinit var repository: CookieSetRepositoryImpl

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        repository = CookieSetRepositoryImpl(dao)
    }

    @Test
    fun saveCookieSetAndDomains_mapModelsToEntities() = runTest {
        val cookieSet = CookieSet(
            id = 4L,
            name = "example",
            source = CookieSetSource.WEBVIEW,
            cookieFilePath = "/cookie.txt",
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
        )
        val cookieSetEntity = CookieSetEntity(
            id = 4L,
            name = "example",
            source = "WEBVIEW",
            cookieFilePath = "/cookie.txt",
            userAgent = null,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            lastUsedAtMillis = null,
        )
        `when`(dao.insertCookieSet(cookieSetEntity))
            .thenReturn(9L)

        assertEquals(9L, repository.saveCookieSet(cookieSet))
        repository.saveDomains(
            listOf(CookieSetDomain(cookieSetId = 9L, domain = "example.com", createdAtMillis = 3L))
        )

        verify(dao).insertCookieSet(cookieSetEntity)
        verify(dao).insertDomains(
            listOf(
                CookieSetDomainEntity(
                    id = 0L,
                    cookieSetId = 9L,
                    domain = "example.com",
                    includeSubdomains = true,
                    createdAtMillis = 3L,
                )
            )
        )
    }

    @Test
    fun emptyCollectionsDoNotQueryDao() = runTest {
        repository.saveDomains(emptyList())

        assertEquals(emptyList<Long>(), repository.getCookieSetIdsByDomains(emptyList()))
        verify(dao, never()).insertDomains(emptyList())
        verify(dao, never()).getCookieSetIdsByDomains(emptyList())
    }

    @Test
    fun readsMapDaoEntitiesAndNulls() = runTest {
        val entity = sampleEntity()
        `when`(dao.getAllWithDomainsAsFlow())
            .thenReturn(flowOf(listOf(entity)))
        `when`(dao.getAllWithDomains())
            .thenReturn(listOf(entity))
        `when`(dao.getByIdWithDomains(8L))
            .thenReturn(entity)
        `when`(dao.getByIdWithDomains(9L))
            .thenReturn(null)

        assertEquals(
            "example",
            repository.getAllWithDomainsAsFlow().first().single().cookieSet.name
        )
        assertEquals("example.com", repository.getAllWithDomains().single().domains.single().domain)
        assertEquals(8L, repository.getByIdWithDomains(8L)?.cookieSet?.id)
        assertNull(repository.getByIdWithDomains(9L))
    }

    @Test
    fun updateAndDeleteMethodsDelegateToDao() = runTest {
        `when`(dao.updateLastUsedAtById(8L, 99L))
            .thenReturn(1)
        `when`(dao.deleteCookieSetById(8L))
            .thenReturn(1)

        repository.updateCookieFilePath(8L, "/new")
        assertEquals(1, repository.updateLastUsedAt(8L, 99L))
        assertEquals(1, repository.deleteCookieSetById(8L))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun sampleEntity() = CookieSetWithDomainsEntity(
        cookieSet = CookieSetEntity(
            id = 8L,
            name = "example",
            source = "IMPORTED_FILE",
            cookieFilePath = "/cookie.txt",
            userAgent = null,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            lastUsedAtMillis = null,
        ),
        domains = listOf(
            CookieSetDomainEntity(
                id = 1L,
                cookieSetId = 8L,
                domain = "example.com",
                includeSubdomains = true,
                createdAtMillis = 1L,
            )
        ),
    )
}
