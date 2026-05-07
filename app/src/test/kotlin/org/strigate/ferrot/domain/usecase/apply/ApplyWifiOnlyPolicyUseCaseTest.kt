package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.integration.DownloadWorkScheduler
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyWifiOnlyPolicyUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

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
    private lateinit var getAllDownloadsUseCase: GetAllDownloadsUseCase

    @Mock
    private lateinit var updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase

    @Mock
    private lateinit var updateDownloadStatusUseCase: UpdateDownloadStatusUseCase

    @Mock
    private lateinit var deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(appContext.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
    }

    @Test
    fun invoke_movesActiveToWaitingForWifi_whenOffWifi() = runTest(testDispatcher) {
        val queued = sampleDownload(41L, DownloadStatus.QUEUED)
        val downloading = sampleDownload(42L, DownloadStatus.DOWNLOADING)
        val completed = sampleDownload(43L, DownloadStatus.COMPLETED)

        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(listOf(queued, downloading, completed))

        stubQuickNetworkProbe(isOnline = true, onWifi = false)
        createUseCase().invoke(true)

        verify(updateDownloadStatusUseCase)
            .invoke(41L, DownloadStatus.WAITING_FOR_WIFI)
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.WAITING_FOR_WIFI)
        verify(updateDownloadErrorMessageUseCase)
            .invoke(41L, null)
        verify(updateDownloadErrorMessageUseCase)
            .invoke(42L, null)
        verify(deleteDownloadFilesUseCase)
            .invoke(41L)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
        verify(updateDownloadStatusUseCase, never())
            .invoke(43L, DownloadStatus.WAITING_FOR_WIFI)
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(41L, true)
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(42L, true)
    }

    @Test
    fun invoke_doesNothing_whenWifiOnlyEnabledOnWifi() = runTest(testDispatcher) {
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(listOf(sampleDownload(44L, DownloadStatus.QUEUED)))

        stubQuickNetworkProbe(isOnline = true, onWifi = true)
        createUseCase().invoke(true)

        verify(updateDownloadStatusUseCase, never())
            .invoke(44L, DownloadStatus.WAITING_FOR_WIFI)
        verify(downloadWorkScheduler, never())
            .enqueueOneTimeReplace(44L, true)
    }

    @Test
    fun invoke_movesWaitingForWifiToNetwork_whenWifiOnlyOff() = runTest(testDispatcher) {
        val waiting = sampleDownload(45L, DownloadStatus.WAITING_FOR_WIFI)
        val queued = sampleDownload(46L, DownloadStatus.QUEUED)

        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(listOf(waiting, queued))

        stubQuickNetworkProbe(isOnline = true, onWifi = false)
        createUseCase().invoke(false)

        verify(updateDownloadErrorMessageUseCase)
            .invoke(45L, null)
        verify(updateDownloadStatusUseCase)
            .invoke(45L, DownloadStatus.WAITING_FOR_NETWORK)
        verify(updateDownloadStatusUseCase, never())
            .invoke(46L, DownloadStatus.WAITING_FOR_NETWORK)
        verify(downloadWorkScheduler)
            .enqueueOneTimeReplace(45L, false)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun stubQuickNetworkProbe(isOnline: Boolean, onWifi: Boolean) {
        `when`(connectivityManager.activeNetwork)
            .thenReturn(network)
        `when`(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        `when`(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(isOnline)
        `when`(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(isOnline)
        `when`(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(onWifi)
    }

    private fun createUseCase() = ApplyWifiOnlyPolicyUseCase(
        appContext = appContext,
        getAllDownloadsUseCase = getAllDownloadsUseCase,
        updateDownloadErrorMessageUseCase = updateDownloadErrorMessageUseCase,
        updateDownloadStatusUseCase = updateDownloadStatusUseCase,
        deleteDownloadFilesUseCase = deleteDownloadFilesUseCase,
        downloadWorkScheduler = downloadWorkScheduler,
    )

    private fun sampleDownload(id: Long, status: DownloadStatus) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = false,
    )
}
