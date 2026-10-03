package org.strigate.ferrot.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.cookieset.CreateCookieSetFromFileUseCase
import org.strigate.ferrot.domain.usecase.cookieset.DeleteCookieSetUseCase
import org.strigate.ferrot.domain.usecase.cookieset.GetCookieSetsWithDomainsAsFlowUseCase
import org.strigate.ferrot.presentation.event.CookiesEvent
import org.strigate.ferrot.presentation.state.CookiesUiState
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CookiesViewModelTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val viewModels = mutableListOf<CookiesViewModel>()

    @Mock
    private lateinit var uri: Uri

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var cookieSetUseCase: CookieSetUseCase

    @Mock
    private lateinit var createCookieSetFromFileUseCase: CreateCookieSetFromFileUseCase

    @Mock
    private lateinit var deleteCookieSetUseCase: DeleteCookieSetUseCase

    @Mock
    private lateinit var getCookieSetsWithDomainsAsFlowUseCase: GetCookieSetsWithDomainsAsFlowUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(getCookieSetsWithDomainsAsFlowUseCase())
            .thenReturn(MutableStateFlow(emptyList()))
        `when`(cookieSetUseCase.createCookieSetFromFileUseCase)
            .thenReturn(createCookieSetFromFileUseCase)
        `when`(cookieSetUseCase.deleteCookieSetUseCase)
            .thenReturn(deleteCookieSetUseCase)
        `when`(cookieSetUseCase.getCookieSetsWithDomainsAsFlowUseCase)
            .thenReturn(getCookieSetsWithDomainsAsFlowUseCase)
    }

    @Test
    fun uiState_mapsCookieSets() = runTest(mainDispatcherRule.testDispatcher) {
        val source = MutableStateFlow(listOf(createCookieSet()))
        `when`(getCookieSetsWithDomainsAsFlowUseCase())
            .thenReturn(source)

        val viewModel = createViewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(3L, (viewModel.uiState.value as CookiesUiState.Data).cookieSets.single().id)
        collector.cancel()
    }

    @Test
    fun uiState_exposesErrorWhenSourceFails() = runTest(mainDispatcherRule.testDispatcher) {
        `when`(getCookieSetsWithDomainsAsFlowUseCase())
            .thenReturn(flow { throw IllegalStateException("failed") })

        val viewModel = createViewModel()
        val collector = backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(CookiesUiState.Error, viewModel.uiState.value)
        collector.cancel()
    }

    @Test
    fun importCookieFile_emitsResultEvents() = runTest(mainDispatcherRule.testDispatcher) {
        `when`(createCookieSetFromFileUseCase("", uri, "", true))
            .thenReturn(createCookieSet())

        val viewModel = createViewModel()
        val success = async(start = CoroutineStart.UNDISPATCHED) { viewModel.event.first() }

        viewModel.importCookieFile(uri)
        advanceUntilIdle()

        assertEquals(CookiesEvent.ShowToast(R.string.toast_cookie_set_saved), success.await())

        `when`(createCookieSetFromFileUseCase("", uri, "", true))
            .thenReturn(null)

        val failure = async(start = CoroutineStart.UNDISPATCHED) { viewModel.event.first() }
        viewModel.importCookieFile(uri)
        advanceUntilIdle()

        assertEquals(CookiesEvent.ShowToast(R.string.toast_cookie_set_failed), failure.await())
    }

    @Test
    fun deleteCookieSet_deletesAndEmitsEvent() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.event.first() }

        viewModel.deleteCookieSet(5L)
        advanceUntilIdle()

        verify(deleteCookieSetUseCase)
            .invoke(5L)

        assertEquals(CookiesEvent.ShowToast(R.string.toast_cookie_set_deleted), event.await())
    }

    @Test
    fun logShown_logsScreen() {
        createViewModel().logShown()
        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.COOKIES)
    }

    @After
    fun tearDown() = runTest(mainDispatcherRule.testDispatcher) {
        viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        autoCloseable.close()
    }

    private fun createViewModel(): CookiesViewModel {
        return CookiesViewModel(analyticsLogger, cookieSetUseCase).also { viewModels += it }
    }

    private fun createCookieSet() = CookieSetWithDomains(
        cookieSet = CookieSet(
            id = 3L,
            name = "example",
            source = CookieSetSource.WEBVIEW,
            cookieFilePath = "/cookie.txt",
        ),
        domains = emptyList(),
    )
}
