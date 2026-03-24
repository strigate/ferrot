package org.strigate.ferrot.presentation

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.repository.DownloadRepository
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.SaveDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var startDownloadUseCase: StartDownloadUseCase

    @Mock
    private lateinit var downloadRepository: DownloadRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        logMock?.close()
        logMock = null
        autoCloseable.close()
    }

    @Test
    fun navigateTo_updatesNavigationEvent() {
        val viewModel = createViewModel()

        viewModel.navigateTo(route = Screen.Settings.route, popUpToDownloads = true)

        assertEquals(
            NavigationEvent.Route(
                route = Screen.Settings.route,
                popUpToDownloads = true,
            ),
            viewModel.navigateRoute.value,
        )
    }

    @Test
    fun navigateTo_defaultsPopUpToDownloadsToFalse() {
        val viewModel = createViewModel()

        viewModel.navigateTo(route = Screen.About.route)

        assertEquals(
            NavigationEvent.Route(route = Screen.About.route),
            viewModel.navigateRoute.value,
        )
    }

    @Test
    fun navigateToDownload_updatesNavigationEvent_whenDownloadExists() = runTest(testDispatcher) {
        val download = Download(
            id = 42L,
            uid = "uid-42",
            url = "https://example.com",
            status = DownloadStatus.QUEUED,
            seen = false,
        )
        val viewModel = createViewModel()
        `when`(downloadRepository.getById(42L))
            .thenReturn(download)

        viewModel.navigateToDownload(42L)
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.Route(
                route = Screen.Download.route(42L),
                popUpToDownloads = true,
            ),
            viewModel.navigateRoute.value,
        )
    }

    @Test
    fun navigateToDownload_keepsNavigationEmpty_whenDownloadDoesNotExist() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            `when`(downloadRepository.getById(99L))
                .thenReturn(null)

            viewModel.navigateToDownload(99L)
            advanceUntilIdle()

            assertNull(viewModel.navigateRoute.value)
        }

    @Test
    fun startDownload_savesQueuedUnseenDownloadAndStartsIt_whenSaveSucceeds() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val savedDownloads = mutableListOf<Download>()
            doAnswer { invocation ->
                savedDownloads += invocation.getArgument<Download>(0)
                12L
            }.`when`(downloadRepository).save(anyObject())

            viewModel.startDownload("https://example.com/video")
            advanceUntilIdle()

            val savedDownload = savedDownloads.single()
            assertEquals("https://example.com/video", savedDownload.url)
            assertEquals(DownloadStatus.QUEUED, savedDownload.status)
            assertFalse(savedDownload.seen)
            assertNotNull(savedDownload.uid)
            assertFalse(savedDownload.uid.isBlank())

            verify(startDownloadUseCase).invoke(12L)
        }

    @Test
    fun startDownload_doesNotStartIt_whenSaveFails() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val savedDownloads = mutableListOf<Download>()
        doAnswer { invocation ->
            savedDownloads += invocation.getArgument<Download>(0)
            -1L
        }.`when`(downloadRepository).save(anyObject())

        viewModel.startDownload("https://example.com/video")
        advanceUntilIdle()

        assertEquals("https://example.com/video", savedDownloads.single().url)
        verifyNoInteractions(startDownloadUseCase)
    }

    @Test
    fun resetNavigate_clearsNavigationEvent() {
        val viewModel = createViewModel()

        viewModel.navigateTo(Screen.About.route)
        viewModel.resetNavigate()

        assertNull(viewModel.navigateRoute.value)
    }

    private fun createViewModel(): MainViewModel {
        val saveDownloadUseCase = SaveDownloadUseCase(downloadRepository)
        val getDownloadByIdUseCase = GetDownloadByIdUseCase(downloadRepository)

        `when`(downloadUseCase.saveDownloadUseCase)
            .thenReturn(saveDownloadUseCase)
        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownloadByIdUseCase)

        return MainViewModel(
            downloadUseCase = downloadUseCase,
            startDownloadUseCase = startDownloadUseCase,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = Mockito.any<T>() ?: null as T
}
