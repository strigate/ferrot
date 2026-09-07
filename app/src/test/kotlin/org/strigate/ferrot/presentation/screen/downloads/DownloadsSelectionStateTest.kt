package org.strigate.ferrot.presentation.screen.downloads

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadsSelectionStateTest {
    @Test
    fun delete_defersVisibleItemsAndImmediatelyDeletesOffscreenItems() {
        val state = DownloadsSelectionState(selectedIds = setOf(1L, 2L, 3L))
        var deletedIds = emptySet<Long>()

        state.deleteSelected(listOf(1L, 3L)) { deletedIds = it }

        assertEquals(setOf(2L), deletedIds)
        assertEquals(setOf(1L, 3L), state.dismissingIds)
        assertEquals(emptySet<Long>(), state.selectedIds)
        state.finishDismiss(1L)
        assertEquals(setOf(3L), state.dismissingIds)
    }

    @Test
    fun archive_preservesPendingDeleteAnimations() {
        val state = DownloadsSelectionState(selectedIds = setOf(1L, 2L), dismissingIds = setOf(3L))
        var archivedIds = emptySet<Long>()

        state.archiveSelected(listOf(1L)) { archivedIds = it }

        assertEquals(setOf(2L), archivedIds)
        assertEquals(setOf(1L), state.archivingIds)
        assertEquals(setOf(3L), state.dismissingIds)
        assertEquals(emptySet<Long>(), state.selectedIds)
        state.finishArchive(1L)
        assertEquals(emptySet<Long>(), state.archivingIds)
    }

    @Test
    fun pruning_preservesSelectionWhileLoadingAndRemovesUnavailableIdsAfterLoading() {
        val state = DownloadsSelectionState(selectedIds = setOf(1L, 2L))

        state.prune(null)
        assertEquals(setOf(1L, 2L), state.selectedIds)
        state.prune(setOf(2L, 3L))
        assertEquals(setOf(2L), state.selectedIds)
    }
}
