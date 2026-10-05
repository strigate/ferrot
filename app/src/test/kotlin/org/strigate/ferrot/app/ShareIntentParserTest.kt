package org.strigate.ferrot.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ShareIntentParserTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var intent: Intent

    @Mock
    private lateinit var extras: Bundle

    @Mock
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun findFirstHttpUrl_extractsAndCleansUrlFromText() {
        val result = ShareIntentParser.findFirstHttpUrl(
            text = "Watch this (https://example.com/video).",
        )

        assertEquals("https://example.com/video", result)
    }

    @Test
    fun findFirstHttpUrl_preservesValidPathPunctuation() {
        listOf(
            "https://example.com/file!",
            "https://example.com/file;",
            "https://example.com/file:",
            "https://example.com/it's-here",
        ).forEach { url ->
            assertEquals(url, ShareIntentParser.findFirstHttpUrl("Shared link: $url"))
        }
    }

    @Test
    fun normalizeHttpUrl_rejectsInvalidSchemesAndHosts() {
        assertNull(ShareIntentParser.normalizeHttpUrl("ftp://example.com/video"))
        assertNull(ShareIntentParser.normalizeHttpUrl("https://"))
        assertNull(ShareIntentParser.normalizeHttpUrl("https://_example.com/video"))
        assertNull(ShareIntentParser.normalizeHttpUrl("https://example.com:65536/video"))
        assertEquals(
            "HTTPS://example.com/video",
            ShareIntentParser.normalizeHttpUrl("HTTPS://example.com/video"),
        )
    }

    @Test
    fun normalizeHttpUrl_preservesPunctuationInUriPayloads() {
        assertEquals(
            "https://example.com/file!",
            ShareIntentParser.normalizeHttpUrl("https://example.com/file!"),
        )
    }

    @Test
    fun normalizeHttpUrl_acceptsInternationalizedHostnames() {
        assertEquals(
            "https://例え.テスト/video",
            ShareIntentParser.normalizeHttpUrl("https://例え.テスト/video"),
        )
    }

    @Test
    fun extractUrl_readsSingleCharSequenceText() {
        `when`(intent.action)
            .thenReturn(Intent.ACTION_SEND)
        `when`(intent.extras)
            .thenReturn(extras)
        `when`(extras.getCharSequence(Intent.EXTRA_TEXT))
            .thenReturn("https://example.com/video")

        assertEquals(
            "https://example.com/video",
            ShareIntentParser.extractUrl(intent),
        )
    }

    @Test
    fun extractUrl_readsSingleStreamUri() {
        `when`(intent.action)
            .thenReturn(Intent.ACTION_SEND)
        `when`(uri.toString())
            .thenReturn("https://example.com/video")

        @Suppress("DEPRECATION")
        `when`(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            .thenReturn(uri)

        assertEquals(
            "https://example.com/video",
            ShareIntentParser.extractUrl(intent),
        )
    }

    @Test
    fun extractUrl_ignoresMultipleShareAction() {
        `when`(intent.action)
            .thenReturn(Intent.ACTION_SEND_MULTIPLE)

        assertNull(ShareIntentParser.extractUrl(intent))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

}
