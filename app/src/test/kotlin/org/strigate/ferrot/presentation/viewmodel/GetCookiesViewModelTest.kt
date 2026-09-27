package org.strigate.ferrot.presentation.viewmodel

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.cookieset.CreateCookieSetFromWebViewUseCase
import org.strigate.ferrot.domain.usecase.cookieset.GetExistingCookieSetDomainForWebViewUrlUseCase
import org.strigate.ferrot.presentation.event.GetCookiesEvent
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class GetCookiesViewModelTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var cookieSetUseCase: CookieSetUseCase

    @Mock
    private lateinit var createCookieSetFromWebViewUseCase: CreateCookieSetFromWebViewUseCase

    @Mock
    private lateinit var getExistingCookieSetDomainForWebViewUrlUseCase: GetExistingCookieSetDomainForWebViewUrlUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(cookieSetUseCase.createCookieSetFromWebViewUseCase)
            .thenReturn(createCookieSetFromWebViewUseCase)
        `when`(cookieSetUseCase.getExistingCookieSetDomainForWebViewUrlUseCase)
            .thenReturn(getExistingCookieSetDomainForWebViewUrlUseCase)
    }

    @Test
    fun logShown_logsScreen() {
        createViewModel().logShown()

        verify(analyticsLogger).logScreen(AnalyticsEvents.Screens.GET_COOKIES)
    }

    @Test
    fun saveCookies_requestsOverwriteConfirmation() = runTest(mainDispatcherRule.testDispatcher) {
        `when`(getExistingCookieSetDomainForWebViewUrlUseCase("https://example.com"))
            .thenReturn("example.com")

        val viewModel = createViewModel()
        val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.event.first() }

        viewModel.saveCookies("https://example.com", "a=b")
        advanceUntilIdle()

        assertEquals(
            GetCookiesEvent.ShowOverwriteConfirmation("https://example.com", "example.com"),
            event.await(),
        )
        verify(createCookieSetFromWebViewUseCase, never()).invoke("https://example.com", "a=b")
    }

    @Test
    fun saveCookies_emitsSavedEvents() = runTest(mainDispatcherRule.testDispatcher) {
        `when`(createCookieSetFromWebViewUseCase("https://example.com", "a=b"))
            .thenReturn(mock(CookieSetWithDomains::class.java))

        val viewModel = createViewModel()
        val events = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.event.take(2).toList()
        }

        viewModel.saveCookies("https://example.com", "a=b", confirmOverwrite = false)
        advanceUntilIdle()

        assertEquals(
            listOf(
                GetCookiesEvent.ShowToast(R.string.toast_cookie_set_saved),
                GetCookiesEvent.Saved,
            ),
            events.await(),
        )
    }

    @Test
    fun saveCookies_emitsFailureOnException() = runTest(mainDispatcherRule.testDispatcher) {
        `when`(createCookieSetFromWebViewUseCase("https://example.com", "a=b"))
            .thenThrow(IllegalStateException("failed"))

        val viewModel = createViewModel()
        val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.event.first() }

        viewModel.saveCookies("https://example.com", "a=b", confirmOverwrite = false)
        advanceUntilIdle()

        assertEquals(GetCookiesEvent.ShowToast(R.string.toast_cookie_set_failed), event.await())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createViewModel() = GetCookiesViewModel(analyticsLogger, cookieSetUseCase)
}
