package org.strigate.ferrot.presentation.screen.downloads

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.strigate.ferrot.R

@Composable
internal fun PendingDeleteSnackbarEffect(
    pendingDeleteIds: Set<Long>,
    snackbarHostState: SnackbarHostState,
    onUndoPendingDelete: (Set<Long>) -> Unit,
    onConfirmPendingDelete: () -> Unit,
) {
    val hasPendingDeletes = pendingDeleteIds.isNotEmpty()

    val snackbarSingleDeleteMessage = stringResource(R.string.snackbar_delete_single_delete)
    val snackbarBulkDeleteMessage = stringResource(R.string.snackbar_bulk_delete_bulk_delete)
    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)

    var snackbarUndoDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val latestUndoPendingDelete by rememberUpdatedState(onUndoPendingDelete)
    val latestConfirmPendingDelete by rememberUpdatedState(onConfirmPendingDelete)
    val latestSnackbarUndoDeleteIds by rememberUpdatedState(snackbarUndoDeleteIds)

    LaunchedEffect(pendingDeleteIds) {
        snackbarUndoDeleteIds = if (pendingDeleteIds.isEmpty()) {
            emptySet()
        } else {
            snackbarUndoDeleteIds + pendingDeleteIds
        }
    }
    LaunchedEffect(hasPendingDeletes) {
        if (!hasPendingDeletes) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val snackbarDeleteIds = snackbarUndoDeleteIds.ifEmpty { pendingDeleteIds.toSet() }
        val snackbarPendingDeleteMessage = getPendingDeleteSnackbarMessage(
            snackbarDeleteIds = snackbarDeleteIds,
            snackbarSingleDeleteMessage = snackbarSingleDeleteMessage,
            snackbarBulkDeleteMessage = snackbarBulkDeleteMessage,
        )
        val snackbarResult = snackbarHostState.showSnackbar(
            message = snackbarPendingDeleteMessage,
            actionLabel = snackbarUndoActionLabel,
            duration = SnackbarDuration.Indefinite,
            withDismissAction = true,
        )
        if (snackbarResult == SnackbarResult.ActionPerformed) {
            latestUndoPendingDelete(latestSnackbarUndoDeleteIds.ifEmpty { snackbarDeleteIds })
            return@LaunchedEffect
        }
        if (snackbarDeleteIds.isNotEmpty()) {
            latestConfirmPendingDelete()
        }
    }
}

private fun getPendingDeleteSnackbarMessage(
    snackbarDeleteIds: Set<Long>,
    snackbarSingleDeleteMessage: String,
    snackbarBulkDeleteMessage: String,
): String {
    val deletedCount = snackbarDeleteIds.size
    return if (deletedCount > 1) {
        "$deletedCount $snackbarBulkDeleteMessage"
    } else {
        snackbarSingleDeleteMessage
    }
}
