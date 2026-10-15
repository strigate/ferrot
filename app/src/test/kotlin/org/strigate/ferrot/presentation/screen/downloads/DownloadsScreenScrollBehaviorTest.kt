package org.strigate.ferrot.presentation.screen.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsScreenScrollBehaviorTest {
    @Test
    fun splitSelectedItemIds_separatesVisibleAndOffscreenSelectedIds() {
        val selectedItemIds = splitSelectedItemIds(
            selectedIds = setOf(1L, 2L, 3L, 4L),
            visibleItemKeys = listOf(9L, 3L, "header", 1L),
        )

        assertEquals(setOf(1L, 3L), selectedItemIds.visible)
        assertEquals(setOf(2L, 4L), selectedItemIds.offscreen)
    }

    @Test
    fun splitSelectedItemIds_keepsEverySelectedIdOffscreen_whenNothingIsVisible() {
        val selectedItemIds = splitSelectedItemIds(
            selectedIds = setOf(1L, 2L),
            visibleItemKeys = listOf(9L, 8L),
        )

        assertTrue(selectedItemIds.visible.isEmpty())
        assertEquals(setOf(1L, 2L), selectedItemIds.offscreen)
    }

    @Test
    fun areAllItemsSelected_requiresEveryAvailableIdToBeSelected() {
        assertTrue(
            areAllItemsSelected(
                selectedIds = setOf(1L, 2L, 3L),
                availableIds = setOf(1L, 2L, 3L),
            ),
        )
        assertFalse(
            areAllItemsSelected(
                selectedIds = setOf(1L, 4L),
                availableIds = setOf(1L, 2L),
            ),
        )
    }

    @Test
    fun pruneSelectedItemIds_removesIdsThatAreNoLongerAvailable() {
        val prunedIds = pruneSelectedItemIds(
            selectedIds = setOf(1L, 2L, 3L),
            availableIds = setOf(2L, 3L, 4L),
        )

        assertEquals(setOf(2L, 3L), prunedIds)
    }

    @Test
    fun pruneSelectedItemIds_keepsIdsWhileDownloadsDataIsUnavailable() {
        val prunedIds = pruneSelectedItemIds(
            selectedIds = setOf(1L, 2L),
            availableIds = null,
        )

        assertEquals(setOf(1L, 2L), prunedIds)
    }

    @Test
    fun hasNewVisibleItem_returnsTrue_whenNewItemIsInsertedAtTop() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertTrue(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsFalse_onInitialLoad() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = emptyList(),
            currentItemIds = listOf(3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsFalse_whenExistingItemMovesToTop() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(2L, 3L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsFalse_whenExistingItemReordersAfterCompletion() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = listOf(10L, 9L, 8L, 7L),
            currentItemIds = listOf(9L, 8L, 7L, 10L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsFalse_whileSearching() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(4L, 3L, 2L, 1L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "test",
        )

        assertFalse(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsTrue_whenNewItemIsAddedNotAtTop() {
        val shouldScroll = hasNewVisibleItem(
            previousItemIds = listOf(3L, 2L, 1L),
            currentItemIds = listOf(3L, 2L, 1L, 4L),
            previousPendingDeleteIds = emptySet(),
            searchQuery = "",
        )

        assertTrue(shouldScroll)
    }

    @Test
    fun hasNewVisibleItem_returnsFalse_whenUndoReAddsPendingDeleteItem() {
        val shouldScroll = hasNewVisibleItem(
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

    @Test
    fun haveItemsReordered_returnsTrue_whenDownloadCompletes() {
        assertTrue(haveItemsReordered(listOf(10L, 9L, 8L, 7L), listOf(9L, 8L, 7L, 10L)))
    }

    @Test
    fun haveItemsReordered_returnsTrue_whenDownloadRetries() {
        assertTrue(haveItemsReordered(listOf(9L, 8L, 7L, 10L), listOf(10L, 9L, 8L, 7L)))
    }

    @Test
    fun haveItemsReordered_returnsTrue_whenAnotherItemIsRemovedDuringCompletion() {
        assertTrue(haveItemsReordered(listOf(10L, 9L, 8L, 7L), listOf(9L, 8L, 10L)))
    }

    @Test
    fun haveItemsReordered_returnsFalse_whenItemsAreOnlyAddedOrRemoved() {
        assertFalse(haveItemsReordered(listOf(3L, 2L, 1L), listOf(3L, 1L)))
        assertFalse(haveItemsReordered(listOf(3L, 1L), listOf(3L, 2L, 1L)))
        assertFalse(haveItemsReordered(emptyList(), listOf(3L, 2L, 1L)))
        assertFalse(haveItemsReordered(listOf(3L, 2L, 1L), emptyList()))
        assertFalse(haveItemsReordered(listOf(3L, 2L, 1L), listOf(3L, 2L, 1L)))
    }
}
