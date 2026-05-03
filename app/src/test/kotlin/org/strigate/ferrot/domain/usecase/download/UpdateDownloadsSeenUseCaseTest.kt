package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.DownloadRepository

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateDownloadsSeenUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var downloadRepository: DownloadRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_updatesRepository_whenIdsArePresent() = runTest {
        val useCase = UpdateDownloadsSeenUseCase(downloadRepository)
        val downloadIds = setOf(1L, 2L, 3L)

        useCase(downloadIds, seen = false)

        verify(downloadRepository)
            .updateSeenByIds(downloadIds, false)
    }

    @Test
    fun invoke_usesSeenTrueByDefault() = runTest {
        val useCase = UpdateDownloadsSeenUseCase(downloadRepository)
        val downloadIds = setOf(4L)

        useCase(downloadIds)

        verify(downloadRepository)
            .updateSeenByIds(downloadIds, true)
    }

    @Test
    fun invoke_doesNothing_whenIdsAreEmpty() = runTest {
        val useCase = UpdateDownloadsSeenUseCase(downloadRepository)

        useCase(emptySet())

        verifyNoInteractions(downloadRepository)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
