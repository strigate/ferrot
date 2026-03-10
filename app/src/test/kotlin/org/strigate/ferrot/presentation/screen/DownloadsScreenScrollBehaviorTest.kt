package org.strigate.ferrot.presentation.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsScreenScrollBehaviorTest {
    @Test
    fun hasNewItemAtTop_returnsTrue_whenNewItemIsInsertedAtTop() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            searchQuery = "",
        )

        assertTrue(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_onInitialLoad() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = emptyList(),
            currentItemIds = listOf(3L, 2L, 1L),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_whenExistingItemMovesToTop() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(2L, 3L, 1L),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_whileSearching() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            searchQuery = "test",
        )

        assertFalse(shouldScroll)
    }
}
