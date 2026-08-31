package org.strigate.ferrot.presentation.screen.downloads

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.helper.InstallHelper
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.AvailableUpdateBanner
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.presentation.util.LifecycleEffect
import org.strigate.ferrot.presentation.util.UiFormatter
import org.strigate.ferrot.presentation.viewmodel.DownloadsViewModel
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_FOCUS_DELAY_MILLIS = 357L
private const val RETRY_FAILED_SCROLL_DELAY_MILLIS = 357L

@Composable
fun DownloadsScreen(
    navController: NavController,
    archived: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isArchived by viewModel.isArchived.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(archived) {
        viewModel.setArchived(archived)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DownloadsEvent.InstallUpdate -> {
                    InstallHelper.requestInstallApkIfExists(context, event.path)
                }
            }
        }
    }
    LifecycleEffect {
        on(Lifecycle.Event.ON_START) {
            viewModel.requestDeletePendingDownloadsImmediate()
        }
    }

    DownloadsScreenContent(
        uiState = uiState,
        searchQuery = searchQuery,
        isArchived = isArchived,
        onUpdateSearchQuery = viewModel::updateSearchQuery,
        onNavigateBack = navController::navigateUp,
        onNavigateToDownload = { item ->
            navController.navigate(
                Screen.Download.route(
                    id = item.id,
                    archived = isArchived,
                )
            )
        },
        onNavigateToArchived = { navController.navigate(Screen.Archived.route) },
        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
        onInstallAvailableUpdate = viewModel::installAvailableUpdate,
        onStopDownload = viewModel::stopDownload,
        onRetryDownload = viewModel::retryDownload,
        onMarkDownloadsPendingDelete = viewModel::markDownloadsPendingDelete,
        onRequestDeletePendingDownloadsImmediate = viewModel::requestDeletePendingDownloadsImmediate,
        onUpdateDownloadsArchived = viewModel::updateDownloadsArchived,
        onToggleDownloadsSeen = viewModel::toggleDownloadsSeen,
        onRetryFailedDownloads = viewModel::retryFailedDownloads,
        onStopAllDownloads = viewModel::stopAllDownloads,
        onToggleGridLayout = viewModel::toggleGridLayoutEnabled,
        modifier = modifier,
    )
}

@Composable
fun ArchivedScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    DownloadsScreen(
        modifier = modifier,
        navController = navController,
        archived = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreenContent(
    uiState: DownloadsUiState,
    searchQuery: String,
    isArchived: Boolean,
    onUpdateSearchQuery: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDownload: (DownloadItemUiData) -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onInstallAvailableUpdate: () -> Unit,
    onStopDownload: (Long) -> Unit,
    onRetryDownload: (Long) -> Unit,
    onMarkDownloadsPendingDelete: (Set<Long>, Boolean) -> Unit,
    onRequestDeletePendingDownloadsImmediate: () -> Unit,
    onUpdateDownloadsArchived: (Set<Long>, Boolean) -> Unit,
    onToggleDownloadsSeen: (Set<Long>) -> Unit,
    onRetryFailedDownloads: () -> Unit,
    onStopAllDownloads: () -> Unit,
    onToggleGridLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var dismissingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var archivingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var snackbarUndoDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val downloadsData = (uiState as? DownloadsUiState.Data)?.data

    val bulkSelectedLabel = stringResource(R.string.bulk_selected)
    val snackbarSingleDeleteMessage = stringResource(R.string.snackbar_delete_single_delete)
    val snackbarBulkDeleteMessage = stringResource(R.string.snackbar_bulk_delete_bulk_delete)

    BackHandler(enabled = searchActive) {
        searchActive = false
        onUpdateSearchQuery("")
        keyboardController?.hide()
    }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(SEARCH_FOCUS_DELAY_MILLIS.milliseconds)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val downloads = downloadsData?.downloads.orEmpty()
    val allIds = downloads.map(DownloadItemUiData::id).toSet()
    val hasDownloads = allIds.isNotEmpty()
    val allSelected = areAllItemsSelected(
        selectedIds = selectedIds,
        availableIds = allIds,
    )
    val selectionMode = selectedIds.isNotEmpty()
    val pendingDeleteIds = downloadsData?.pendingDeleteIds.orEmpty()
    val hasPendingDeletes = pendingDeleteIds.isNotEmpty()
    val retryFailedDownloadIds = downloadsData?.retryFailedDownloadIds.orEmpty()
    val hasFailedDownloads = retryFailedDownloadIds.isNotEmpty()
    val hasActiveDownloads = remember(downloads) {
        downloads.any { it.status.isActive }
    }
    val selectedBytes = remember(downloads, selectedIds) {
        downloads
            .asSequence()
            .filter { it.id in selectedIds }
            .sumOf { it.bytesDownloaded }
    }
    val selectionCountTitle = remember(selectedIds, bulkSelectedLabel) {
        buildString {
            append(selectedIds.size)
            append(' ')
            append(bulkSelectedLabel)
        }
    }
    val selectionSizeTitle = remember(selectedBytes) {
        UiFormatter.formatBytes(selectedBytes)
    }
    val shouldMarkSelectionSeen = remember(downloads, selectedIds) {
        downloads.any { it.id in selectedIds && !it.seen }
    }

    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)
    val onScrollToTop: () -> Unit = {
        coroutineScope.launch {
            lazyGridState.animateScrollToItem(0)
        }
    }
    val onRetryFailedAndScrollToTop: () -> Unit = {
        onRetryFailedDownloads()
        coroutineScope.launch {
            delay(RETRY_FAILED_SCROLL_DELAY_MILLIS.milliseconds)
            lazyGridState.animateScrollToItem(0)
        }
    }

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }
    LaunchedEffect(downloadsData != null, allIds) {
        selectedIds = pruneSelectedItemIds(
            selectedIds = selectedIds,
            availableIds = allIds.takeIf { downloadsData != null },
        )
    }
    PendingDeleteSnackbarEffect(
        pendingDeleteIds = pendingDeleteIds,
        hasPendingDeletes = hasPendingDeletes,
        snackbarUndoDeleteIds = snackbarUndoDeleteIds,
        onSnackbarUndoDeleteIdsChange = { snackbarUndoDeleteIds = it },
        snackbarHostState = snackbarHostState,
        snackbarSingleDeleteMessage = snackbarSingleDeleteMessage,
        snackbarBulkDeleteMessage = snackbarBulkDeleteMessage,
        snackbarUndoActionLabel = snackbarUndoActionLabel,
        onUndoPendingDelete = { ids ->
            onMarkDownloadsPendingDelete(ids, false)
        },
        onConfirmPendingDelete = onRequestDeletePendingDownloadsImmediate,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                DownloadsSelectionTopBar(
                    selectionCountTitle = selectionCountTitle,
                    selectionSizeTitle = selectionSizeTitle,
                    shouldMarkSelectionSeen = shouldMarkSelectionSeen,
                    isArchived = isArchived,
                    onClearSelection = { selectedIds = emptySet() },
                    onToggleAll = {
                        selectedIds = if (allSelected) emptySet() else allIds
                    },
                    onToggleSeen = { onToggleDownloadsSeen(selectedIds) },
                    onArchive = {
                        val selectedItemIds = splitSelectedItemIds(
                            selectedIds = selectedIds,
                            visibleItemKeys = lazyGridState.layoutInfo.visibleItemsInfo.map { it.key },
                        )
                        archivingIds += selectedItemIds.visible
                        if (selectedItemIds.offscreen.isNotEmpty()) {
                            onUpdateDownloadsArchived(selectedItemIds.offscreen, !isArchived)
                        }
                        selectedIds = emptySet()
                    },
                    onDelete = {
                        val selectedItemIds = splitSelectedItemIds(
                            selectedIds = selectedIds,
                            visibleItemKeys = lazyGridState.layoutInfo.visibleItemsInfo.map { it.key },
                        )
                        dismissingIds += selectedItemIds.visible
                        if (selectedItemIds.offscreen.isNotEmpty()) {
                            onMarkDownloadsPendingDelete(selectedItemIds.offscreen, true)
                        }
                        selectedIds = emptySet()
                    },
                )
            } else {
                DownloadsDefaultTopBar(
                    hasDownloads = hasDownloads,
                    hasFailedDownloads = hasFailedDownloads,
                    hasActiveDownloads = hasActiveDownloads,
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                    isArchived = isArchived,
                    gridLayoutEnabled = downloadsData?.gridLayoutEnabled ?: false,
                    searchFocusRequester = searchFocusRequester,
                    onSearchActiveChange = { searchActive = it },
                    onSearchQueryChange = onUpdateSearchQuery,
                    onNavigateBack = onNavigateBack,
                    onNavigateToArchived = onNavigateToArchived,
                    onNavigateToSettings = onNavigateToSettings,
                    onSelectAll = { selectedIds = allIds },
                    onScrollToTop = onScrollToTop,
                    onRetryFailedAndScrollToTop = onRetryFailedAndScrollToTop,
                    onStopAllDownloads = onStopAllDownloads,
                    onToggleGridLayout = onToggleGridLayout,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                    dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val state = uiState) {
                is DownloadsUiState.Loading -> {
                    LoadingState(
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.Center,
                    )
                }

                is DownloadsUiState.Data -> {
                    with(state.data) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            availableUpdate?.let {
                                AvailableUpdateBanner(
                                    modifier = Modifier.fillMaxWidth(),
                                    tag = it.tag,
                                    localFilePath = it.localFilePath,
                                    onClick = { onInstallAvailableUpdate() },
                                )
                            }
                            DownloadsContent(
                                items = downloads,
                                selectedIds = selectedIds,
                                dismissingIds = dismissingIds,
                                archivingIds = archivingIds,
                                pendingDeleteIds = pendingDeleteIds,
                                archived = isArchived,
                                leftSwipeAction = leftSwipeAction,
                                rightSwipeAction = rightSwipeAction,
                                hasAvailableUpdateBanner = availableUpdate != null,
                                searchQuery = searchQuery,
                                gridLayoutEnabled = gridLayoutEnabled,
                                lazyGridState = lazyGridState,
                                onItemClick = { item ->
                                    if (pendingDeleteIds.isNotEmpty()) {
                                        onRequestDeletePendingDownloadsImmediate()
                                    }
                                    keyboardController?.hide()
                                    onNavigateToDownload(item)
                                },
                                onPauseResume = { item ->
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    if (item.status.isActive) {
                                        onStopDownload(item.id)
                                    } else {
                                        onRetryDownload(item.id)
                                    }
                                },
                                onSelectionChange = {
                                    selectedIds = it
                                },
                                onBulkDismissAnimationFinished = { itemId ->
                                    dismissingIds = dismissingIds - itemId
                                    onMarkDownloadsPendingDelete(setOf(itemId), true)
                                },
                                onBulkArchiveAnimationFinished = { itemId ->
                                    archivingIds = archivingIds - itemId
                                    onUpdateDownloadsArchived(setOf(itemId), !isArchived)
                                },
                                onToggleSeen = { itemId ->
                                    onToggleDownloadsSeen(setOf(itemId))
                                },
                                onMarkPendingDelete = { ids ->
                                    onMarkDownloadsPendingDelete(ids, true)
                                },
                            )
                        }
                    }
                }

                is DownloadsUiState.Error -> DownloadsError()
            }
        }
    }
}

@Composable
private fun PendingDeleteSnackbarEffect(
    pendingDeleteIds: Set<Long>,
    hasPendingDeletes: Boolean,
    snackbarUndoDeleteIds: Set<Long>,
    onSnackbarUndoDeleteIdsChange: (Set<Long>) -> Unit,
    snackbarHostState: SnackbarHostState,
    snackbarSingleDeleteMessage: String,
    snackbarBulkDeleteMessage: String,
    snackbarUndoActionLabel: String,
    onUndoPendingDelete: (Set<Long>) -> Unit,
    onConfirmPendingDelete: () -> Unit,
) {
    val latestSnackbarUndoDeleteIds by rememberUpdatedState(snackbarUndoDeleteIds)

    LaunchedEffect(pendingDeleteIds) {
        onSnackbarUndoDeleteIdsChange(
            if (pendingDeleteIds.isEmpty()) {
                emptySet()
            } else {
                snackbarUndoDeleteIds + pendingDeleteIds
            }
        )
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
            onUndoPendingDelete(latestSnackbarUndoDeleteIds.ifEmpty { snackbarDeleteIds })
            return@LaunchedEffect
        }
        if (snackbarDeleteIds.isNotEmpty()) {
            onConfirmPendingDelete()
        }
    }
}

@Composable
private fun DownloadsError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier.fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_downloads),
    )
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
