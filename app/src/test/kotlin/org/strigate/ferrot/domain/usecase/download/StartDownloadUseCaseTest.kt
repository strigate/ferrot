package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.integration.DownloadWorkScheduler
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.settings.GetDownloadWifiOnlySettingAsFlowUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class StartDownloadUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable
    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    @Mock
    private lateinit var network: Network

    @Mock
    private lateinit var networkCapabilities: NetworkCapabilities

    @Mock
    private lateinit var downloadWorkScheduler: DownloadWorkScheduler

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase

    @Mock
    private lateinit var getDownloadWifiOnlySettingAsFlowUseCase: GetDownloadWifiOnlySettingAsFlowUseCase

    @Mock
    private lateinit var updateDownloadStatusUseCase: UpdateDownloadStatusUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)

        `when`(settingsUseCase.getDownloadWifiOnlySettingAsFlowUseCase)
            .thenReturn(getDownloadWifiOnlySettingAsFlowUseCase)
        `when`(downloadUseCase.updateDownloadStatusUseCase)
            .thenReturn(updateDownloadStatusUseCase)
        `when`(appContext.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
    }

    @Test
    fun invoke_setsWaitingForNetwork_whenOffline() = runTest(testDispatcher) {
        stubWifiOnly(enabled = false)
        stubNetwork(hasInternet = false, onWifi = false)

        createUseCase().invoke(11L)

        verify(clearNotificationsByDownloadIdUseCase)
            .invoke(11L)
        verify(updateDownloadStatusUseCase)
            .invoke(
                downloadId = 11L,
                status = DownloadStatus.WAITING_FOR_NETWORK,
            )
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(11L, false)
    }

    @Test
    fun invoke_setsWaitingForWifi_whenWifiOnlyAndNotOnWifi() = runTest(testDispatcher) {
        stubWifiOnly(enabled = true)
        stubNetwork(hasInternet = true, onWifi = false)

        createUseCase().invoke(12L)

        verify(updateDownloadStatusUseCase)
            .invoke(
                downloadId = 12L,
                status = DownloadStatus.WAITING_FOR_WIFI,
            )
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(12L, true)
    }

    @Test
    fun invoke_setsQueued_whenDownloadCanStart() = runTest(testDispatcher) {
        stubWifiOnly(enabled = true)
        stubNetwork(hasInternet = true, onWifi = true)

        createUseCase().invoke(13L)

        verify(updateDownloadStatusUseCase)
            .invoke(
                downloadId = 13L,
                status = DownloadStatus.QUEUED,
            )
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(13L, true)
    }

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

    private fun stubWifiOnly(enabled: Boolean) {
        `when`(getDownloadWifiOnlySettingAsFlowUseCase.invoke())
            .thenReturn(flowOf(enabled))
    }

    private fun stubNetwork(hasInternet: Boolean, onWifi: Boolean) {
        `when`(connectivityManager.activeNetwork)
            .thenReturn(network)
        `when`(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        `when`(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(hasInternet)
        `when`(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(hasInternet)
        `when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(onWifi)
        if (!hasInternet) {
            @Suppress("DEPRECATION")
            `when`(connectivityManager.allNetworks)
                .thenReturn(emptyArray())
        }
    }

    private fun createUseCase() = StartDownloadUseCase(
        appContext = appContext,
        settingsUseCase = settingsUseCase,
        downloadUseCase = downloadUseCase,
        clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
        downloadWorkScheduler = downloadWorkScheduler,
    )
}
