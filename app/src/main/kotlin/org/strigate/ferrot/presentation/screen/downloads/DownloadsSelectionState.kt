package org.strigate.ferrot.presentation.screen.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

internal class DownloadsSelectionState(
    selectedIds: Set<Long> = emptySet(),
    dismissingIds: Set<Long> = emptySet(),
    archivingIds: Set<Long> = emptySet(),
) {
    var selectedIds by mutableStateOf(selectedIds)
    var dismissingIds by mutableStateOf(dismissingIds)
        private set
    var archivingIds by mutableStateOf(archivingIds)
        private set

    fun prune(availableIds: Set<Long>?) {
        selectedIds = pruneSelectedItemIds(selectedIds, availableIds)
    }

    fun archiveSelected(visibleItemKeys: List<Any>, onArchive: (Set<Long>) -> Unit) {
        val selection = splitSelectedItemIds(selectedIds, visibleItemKeys)
        archivingIds += selection.visible
        if (selection.offscreen.isNotEmpty()) {
            onArchive(selection.offscreen)
        }
        selectedIds = emptySet()
    }

    fun deleteSelected(visibleItemKeys: List<Any>, onDelete: (Set<Long>) -> Unit) {
        val selection = splitSelectedItemIds(selectedIds, visibleItemKeys)
        dismissingIds += selection.visible
        if (selection.offscreen.isNotEmpty()) {
            onDelete(selection.offscreen)
        }
        selectedIds = emptySet()
    }

    fun finishDismiss(itemId: Long) {
        dismissingIds -= itemId
    }

    fun finishArchive(itemId: Long) {
        archivingIds -= itemId
    }

    companion object {
        val Saver = listSaver<DownloadsSelectionState, LongArray>(
            save = {
                listOf(
                    it.selectedIds.toLongArray(),
                    it.dismissingIds.toLongArray(),
                    it.archivingIds.toLongArray()
                )
            },
            restore = { DownloadsSelectionState(it[0].toSet(), it[1].toSet(), it[2].toSet()) },
        )
    }
}

internal fun splitSelectedItemIds(
    selectedIds: Set<Long>,
    visibleItemKeys: List<Any>,
): SelectedItemIds {
    if (selectedIds.isEmpty() || visibleItemKeys.isEmpty()) {
        return SelectedItemIds(
            visible = emptySet(),
            offscreen = selectedIds,
        )
    }
    val visibleIds = visibleItemKeys
        .filterIsInstance<Long>()
        .toSet()
    val visibleSelectedIds = selectedIds.intersect(visibleIds)

    return SelectedItemIds(
        visible = visibleSelectedIds,
        offscreen = selectedIds - visibleSelectedIds,
    )
}

internal fun areAllItemsSelected(
    selectedIds: Set<Long>,
    availableIds: Set<Long>,
): Boolean {
    return availableIds.isNotEmpty() && availableIds.all(selectedIds::contains)
}

internal fun pruneSelectedItemIds(
    selectedIds: Set<Long>,
    availableIds: Set<Long>?,
): Set<Long> {
    return availableIds?.let(selectedIds::intersect) ?: selectedIds
}

internal data class SelectedItemIds(
    val visible: Set<Long>,
    val offscreen: Set<Long>,
)
