package org.strigate.ferrot.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsScreenScrollBehaviorTest {
    @Test
    fun hasNewItemAtTop_returnsTrue_whenNewItemIsInsertedAtTop() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertTrue(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_onInitialLoad() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = emptyList(),
            currentItemIds = listOf(3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_whenExistingItemMovesToTop() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(2L, 3L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_whileSearching() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "test",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsTrue_whenNewItemIsAddedNotAtTop() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(3L, 2L, 1L, 4L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertTrue(shouldScroll)
    }

    @Test
    fun hasNewItemAtTop_returnsFalse_whenUndoReAddsPendingDeleteItem() {
        val shouldScroll = hasNewItemAtTop(
            previousItemIds = listOf(3L, 2L),
            currentItemIds = listOf(3L, 2L, 1L),
            previousPendingDeleteIds = setOf(1L),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun getRestoredItemIds_returnsOnlyDownloadsThatWerePendingDeleteAndAreVisibleAgain() {
        val restoredIds = getRestoredItemIds(
            previousPendingDeleteIds = setOf(2L, 3L, 4L),
            currentItemIds = listOf(1L, 2L, 4L, 5L),
            currentPendingDeleteIds = setOf(4L),
        )

        assertEquals(setOf(2L), restoredIds)
    }
}
