package org.strigate.ferrot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity
import org.strigate.ferrot.domain.model.AvailableUpdate

@OptIn(ExperimentalCoroutinesApi::class)
class AvailableUpdateRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var availableUpdateDao: AvailableUpdateDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun getAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(availableUpdateDao.get())
            .thenReturn(
                flowOf(
                    AvailableUpdateEntity(
                        id = 0,
                        tag = "v1.2.3",
                        localFilePath = "/tmp/update.apk",
                    ),
                ),
            )

        val repository = AvailableUpdateRepositoryImpl(availableUpdateDao)
        val result = repository.getAsFlow().value()

        assertEquals(
            AvailableUpdate(
                tag = "v1.2.3",
                localFilePath = "/tmp/update.apk",
            ),
            result,
        )
    }

    @Test
    fun getAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(availableUpdateDao.get())
            .thenReturn(flowOf(null))

        val repository = AvailableUpdateRepositoryImpl(availableUpdateDao)

        assertNull(repository.getAsFlow().value())
    }

    @Test
    fun save_insertsMappedEntity() = runTest(testDispatcher) {
        val repository = AvailableUpdateRepositoryImpl(availableUpdateDao)
        val update = AvailableUpdate(
            tag = "v2.0.0",
            localFilePath = "/updates/update.apk",
        )

        repository.save(update)

        verify(availableUpdateDao)
            .insertReplace(
                AvailableUpdateEntity(
                    id = 0,
                    tag = "v2.0.0",
                    localFilePath = "/updates/update.apk",
                ),
            )
    }

    @Test
    fun delete_returnsDaoDeleteCount() = runTest(testDispatcher) {
        `when`(availableUpdateDao.delete())
            .thenReturn(1)

        val repository = AvailableUpdateRepositoryImpl(availableUpdateDao)
        val result = repository.delete()

        assertEquals(1, result)
        verify(availableUpdateDao)
            .delete()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.value(): T = first()
}
