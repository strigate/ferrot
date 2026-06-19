package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.R
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.ClearAvailableUpdateFilesAndDataUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.GetAvailableUpdateAsFlowUseCase
import org.strigate.ferrot.domain.usecase.state.SaveLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.test.MainDispatcherRule
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadAvailableUpdateWorkerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var notificationService: NotificationService

    @Mock
    private lateinit var stateUseCase: StateUseCase

    @Mock
    private lateinit var updatePathProvider: UpdatePathProvider

    @Mock
    private lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    @Mock
    private lateinit var getAvailableUpdateAsFlowUseCase: GetAvailableUpdateAsFlowUseCase

    @Mock
    private lateinit var clearAvailableUpdateFilesAndDataUseCase: ClearAvailableUpdateFilesAndDataUseCase

    @Mock
    private lateinit var saveLastAvailableUpdateCheckMillisUseCase: SaveLastAvailableUpdateCheckMillisUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)
        TestUrlHandler.responseJson = "{}"
        `when`(appContext.getString(R.string.github_latest_release))
            .thenReturn("test://latest-release")
        `when`(availableUpdateUseCase.getAvailableUpdateAsFlowUseCase)
            .thenReturn(getAvailableUpdateAsFlowUseCase)
        `when`(availableUpdateUseCase.clearAvailableUpdateFilesAndDataUseCase)
            .thenReturn(clearAvailableUpdateFilesAndDataUseCase)
        `when`(stateUseCase.saveLastAvailableUpdateCheckMillisUseCase)
            .thenReturn(saveLastAvailableUpdateCheckMillisUseCase)
        `when`(getAvailableUpdateAsFlowUseCase.invoke())
            .thenReturn(flowOf(null))
        `when`(updatePathProvider.apkFileFor("v9.9.9"))
            .thenReturn(File(temporaryFolder.root, "ferrot-v9.9.9.apk"))
    }

    @Test
    fun doWork_clearsSavedUpdateAndMarksCheckSuccessful_whenLatestReleaseIsDraft() =
        runTest(testDispatcher) {
            TestUrlHandler.responseJson = """
            {
              "tag_name": "v9.9.9",
              "draft": true,
              "prerelease": false,
              "assets": []
            }
        """.trimIndent()
            `when`(getAvailableUpdateAsFlowUseCase.invoke())
                .thenReturn(flowOf(AvailableUpdate(tag = "v9.9.8", localFilePath = null)))

            val result = createWorker().doWork()

            assertTrue(
                "Expected success, was ${result.javaClass.name}: $result",
                result is ListenableWorker.Result.Success,
            )
            verify(clearAvailableUpdateFilesAndDataUseCase).invoke()
            verify(saveLastAvailableUpdateCheckMillisUseCase).invoke(anyLong())
        }

    @Test
    fun doWork_clearsSavedUpdateAndMarksCheckSuccessful_whenNoApkAssetExists() =
        runTest(testDispatcher) {
            TestUrlHandler.responseJson = """
            {
              "tag_name": "v9.9.9",
              "draft": false,
              "prerelease": false,
              "assets": [
                { "name": "notes.txt", "browser_download_url": "test://notes", "size": 5 }
              ]
            }
        """.trimIndent()

            val result = createWorker().doWork()

            assertTrue(
                "Expected success, was ${result.javaClass.name}: $result",
                result is ListenableWorker.Result.Success,
            )
            verify(clearAvailableUpdateFilesAndDataUseCase).invoke()
            verify(saveLastAvailableUpdateCheckMillisUseCase).invoke(anyLong())
        }

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

    private fun createWorker() = DownloadAvailableUpdateWorker(
        appContext = appContext,
        workerParameters = mockWorkerParameters(),
        notificationService = notificationService,
        stateUseCase = stateUseCase,
        updatePathProvider = updatePathProvider,
        availableUpdateUseCase = availableUpdateUseCase,
    )

    private fun mockWorkerParameters(
        inputData: Data = Data.EMPTY,
        runAttemptCount: Int = 0,
    ): WorkerParameters {
        val workerParameters = mock(WorkerParameters::class.java)
        `when`(workerParameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(workerParameters.inputData)
            .thenReturn(inputData)
        `when`(workerParameters.runAttemptCount)
            .thenReturn(runAttemptCount)
        return workerParameters
    }

    private class TestConnection(url: URL) : HttpURLConnection(url) {
        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = HTTP_OK

        override fun getInputStream(): InputStream = ByteArrayInputStream(
            TestUrlHandler.responseJson.toByteArray()
        )
    }

    private object TestUrlHandler : URLStreamHandler() {
        var responseJson: String = "{}"

        override fun openConnection(url: URL): URLConnection = TestConnection(url)
    }

    private class TestUrlHandlerFactory : URLStreamHandlerFactory {
        override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
            return if (protocol == "test") TestUrlHandler else null
        }
    }

    companion object {
        init {
            runCatching {
                URL.setURLStreamHandlerFactory(TestUrlHandlerFactory())
            }
        }
    }
}
