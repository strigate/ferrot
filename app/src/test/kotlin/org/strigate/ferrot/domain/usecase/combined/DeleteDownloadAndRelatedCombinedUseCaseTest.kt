package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.DeleteDownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.DeleteDownloadMetadataByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.DeleteDownloadProgressByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.DeleteDownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteDownloadAndRelatedCombinedUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var downloadAudioUseCase: DownloadAudioUseCase

    @Mock
    private lateinit var downloadVideoUseCase: DownloadVideoUseCase

    @Mock
    private lateinit var downloadProgressUseCase: DownloadProgressUseCase

    @Mock
    private lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Mock
    private lateinit var clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase

    @Mock
    private lateinit var deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase

    @Mock
    private lateinit var deleteDownloadMetadataByDownloadIdUseCase: DeleteDownloadMetadataByDownloadIdUseCase

    @Mock
    private lateinit var deleteDownloadProgressByDownloadIdUseCase: DeleteDownloadProgressByDownloadIdUseCase

    @Mock
    private lateinit var deleteDownloadAudioUseCase: DeleteDownloadAudioUseCase

    @Mock
    private lateinit var deleteDownloadVideoUseCase: DeleteDownloadVideoUseCase

    @Mock
    private lateinit var deleteDownloadByIdUseCase: DeleteDownloadByIdUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(downloadUseCase.deleteDownloadFilesUseCase)
            .thenReturn(deleteDownloadFilesUseCase)
        `when`(downloadUseCase.deleteDownloadByIdUseCase)
            .thenReturn(deleteDownloadByIdUseCase)
        `when`(downloadMetadataUseCase.deleteDownloadMetadataByDownloadIdUseCase)
            .thenReturn(deleteDownloadMetadataByDownloadIdUseCase)
        `when`(downloadProgressUseCase.deleteDownloadProgressByDownloadIdUseCase)
            .thenReturn(deleteDownloadProgressByDownloadIdUseCase)
        `when`(downloadAudioUseCase.deleteDownloadAudioUseCase)
            .thenReturn(deleteDownloadAudioUseCase)
        `when`(downloadVideoUseCase.deleteDownloadVideoUseCase)
            .thenReturn(deleteDownloadVideoUseCase)
    }

    @Test
    fun invoke_returnsTrue_whenAllDeletesSucceed() = runTest(testDispatcher) {
        stubDeletes(
            files = true,
            metadata = true,
            progress = true,
            audio = true,
            video = true,
            download = true,
        )

        val result = createUseCase().invoke(31L)

        assertTrue(result)
        verifyDeleteSequence(31L)
    }

    @Test
    fun invoke_returnsFalse_whenAnyDeleteFails() = runTest(testDispatcher) {
        stubDeletes(
            files = true,
            metadata = false,
            progress = true,
            audio = true,
            video = true,
            download = true
        )

        val result = createUseCase().invoke(32L)

        assertFalse(result)
        verify(clearNotificationsByDownloadIdUseCase)
            .invoke(32L)
        verify(deleteDownloadByIdUseCase)
            .invoke(32L)
    }

    private suspend fun stubDeletes(
        files: Boolean,
        metadata: Boolean,
        progress: Boolean,
        audio: Boolean,
        video: Boolean,
        download: Boolean,
    ) {
        `when`(deleteDownloadFilesUseCase.invoke(anyLong()))
            .thenReturn(files)
        `when`(deleteDownloadMetadataByDownloadIdUseCase.invoke(anyLong()))
            .thenReturn(metadata)
        `when`(deleteDownloadProgressByDownloadIdUseCase.invoke(anyLong()))
            .thenReturn(progress)
        `when`(deleteDownloadAudioUseCase.invoke(anyLong()))
            .thenReturn(audio)
        `when`(deleteDownloadVideoUseCase.invoke(anyLong()))
            .thenReturn(video)
        `when`(deleteDownloadByIdUseCase.invoke(anyLong()))
            .thenReturn(download)
    }

    private suspend fun verifyDeleteSequence(downloadId: Long) {
        val inOrder: InOrder = inOrder(
            clearNotificationsByDownloadIdUseCase,
            deleteDownloadFilesUseCase,
            deleteDownloadMetadataByDownloadIdUseCase,
            deleteDownloadProgressByDownloadIdUseCase,
            deleteDownloadAudioUseCase,
            deleteDownloadVideoUseCase,
            deleteDownloadByIdUseCase,
        )

        inOrder.verify(clearNotificationsByDownloadIdUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadFilesUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadMetadataByDownloadIdUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadProgressByDownloadIdUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadAudioUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadVideoUseCase)
            .invoke(downloadId)
        inOrder.verify(deleteDownloadByIdUseCase)
            .invoke(downloadId)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = DeleteDownloadAndRelatedCombinedUseCase(
        downloadUseCase = downloadUseCase,
        downloadAudioUseCase = downloadAudioUseCase,
        downloadVideoUseCase = downloadVideoUseCase,
        downloadProgressUseCase = downloadProgressUseCase,
        downloadMetadataUseCase = downloadMetadataUseCase,
        clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
    )

    private fun anyLong(): Long = org.mockito.Mockito.anyLong()
}
