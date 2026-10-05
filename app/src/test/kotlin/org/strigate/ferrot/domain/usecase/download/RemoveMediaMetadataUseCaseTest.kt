package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.integration.MediaToolsClient
import java.io.File
import java.io.IOException

class RemoveMediaMetadataUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Mock
    private lateinit var client: MediaToolsClient

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun processingFailurePreservesOriginal() = runTest {
        val file = temporaryFolder.newFile("original.mp4").apply {
            writeText("original content")
        }

        `when`(client.probe(file))
            .thenReturn(VIDEO_INFO)
        doAnswer { throw IOException("failed") }
            .`when`(client)
            .remux(anyList())

        val failure = runCatching { RemoveMediaMetadataUseCase(client)(file) }.exceptionOrNull()

        assertTrue(failure is MetadataRemovalException)
        assertEquals("original content", file.readText())
        assertEquals(listOf(file.name), temporaryFolder.root.listFiles()!!.map { it.name })
    }

    @Test
    fun validationFailurePreservesOriginal() = runTest {
        val file = temporaryFolder.newFile("original.mp4").apply { writeText("original content") }
        val originalBytes = file.readBytes()

        `when`(client.probe(any(File::class.java) ?: file))
            .thenAnswer { invocation ->
                if (invocation.getArgument<File>(0) == file) {
                    VIDEO_INFO
                } else {
                    VIDEO_INFO.replace("\"width\":64", "\"width\":32")
                }
            }
        doAnswer { invocation ->
            val arguments = invocation.getArgument<List<String>>(0)
            File(arguments.last()).writeText("broken output")
            null
        }.`when`(client).remux(anyList())

        val failure = runCatching { RemoveMediaMetadataUseCase(client)(file) }.exceptionOrNull()

        assertTrue(failure is MetadataRemovalException)
        val rootCause = generateSequence(failure) { it.cause }.last()
        assertTrue(rootCause is IllegalArgumentException)
        assertArrayEquals(originalBytes, file.readBytes())
        assertEquals(1, temporaryFolder.root.listFiles()!!.size)
    }

    @Test
    fun cancellationIsNotConvertedIntoProcessingFailure() = runTest {
        val file = temporaryFolder.newFile("original.mp4").apply {
            writeText("original content")
        }
        `when`(client.probe(file))
            .thenReturn(VIDEO_INFO)
        doAnswer { throw CancellationException("stopped") }
            .`when`(client)
            .remux(anyList())

        val failure = runCatching { RemoveMediaMetadataUseCase(client)(file) }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals("original content", file.readText())
        assertEquals(1, temporaryFolder.root.listFiles()!!.size)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    companion object {
        private const val VIDEO_INFO =
            """{"streams":[{"index":0,"codec_type":"video","codec_name":"h264","width":64,"height":64}],"format":{"duration":"1.0"}}"""
    }
}
