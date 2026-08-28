package org.strigate.ferrot.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.helper.InstallHelper
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.AvailableUpdateBanner
import org.strigate.ferrot.presentation.component.DownloadPrimaryActionButton
import org.strigate.ferrot.presentation.component.DownloadProgressSection
import org.strigate.ferrot.presentation.component.state.EmptyState
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.theme.TextStyles
import org.strigate.ferrot.presentation.transitions.Transitions
import org.strigate.ferrot.presentation.util.LifecycleEffect
import org.strigate.ferrot.presentation.util.UiFormatter
import org.strigate.ferrot.presentation.viewmodel.DownloadsViewModel
import org.strigate.refinery.theme.RefineryTopAppBarDefaults
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_FOCUS_DELAY_MILLIS = 357L
private const val RETRY_FAILED_SCROLL_DELAY_MILLIS = 357L
private const val SNAP_BACK_SWIPE_THRESHOLD_RATIO = 0.3f
private const val DISMISS_SWIPE_THRESHOLD_RATIO = 0.5f
private const val GRID_SWIPE_ANIMATION_MILLIS = 280
private const val GRID_SWIPE_PLACEMENT_DELAY_MILLIS = 360L
private const val LIST_SWIPE_PLACEMENT_DELAY_MILLIS = 120L

private val DOWNLOAD_GRID_MIN_CELL_SIZE = 160.dp
private val GRID_SWIPE_VELOCITY_THRESHOLD = 125.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreenContent(
    uiState: DownloadsUiState,
    searchQuery: TextFieldValue,
    isArchived: Boolean,
    onUpdateSearchQuery: (TextFieldValue) -> Unit,
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
    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var dismissingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var archivingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var snackbarUndoDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    val bulkSelectedLabel = stringResource(R.string.bulk_selected)
    val snackbarSingleDeleteMessage = stringResource(R.string.snackbar_delete_single_delete)
    val snackbarBulkDeleteMessage = stringResource(R.string.snackbar_bulk_delete_bulk_delete)

    BackHandler(enabled = searchActive) {
        searchActive = false
        onUpdateSearchQuery(TextFieldValue(""))
        keyboardController?.hide()
    }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(SEARCH_FOCUS_DELAY_MILLIS.milliseconds)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val allIds = when (val state = uiState) {
        is DownloadsUiState.Data -> state.data.downloads.map { it.id }.toSet()
        else -> emptySet()
    }
    val hasDownloads = allIds.isNotEmpty()
    val allSelected = selectedIds.isNotEmpty() && selectedIds.size == allIds.size
    val selectionMode = selectedIds.isNotEmpty()
    val pendingDeleteIds = (uiState as? DownloadsUiState.Data)
        ?.data
        ?.pendingDeleteIds ?: emptySet()
    val hasPendingDeletes = pendingDeleteIds.isNotEmpty()
    val retryFailedDownloadIds = (uiState as? DownloadsUiState.Data)
        ?.data
        ?.retryFailedDownloadIds ?: emptySet()

    val hasFailedDownloads = retryFailedDownloadIds.isNotEmpty()
    val hasActiveDownloads = remember(uiState) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
        downloads.any { it.status.isActive }
    }
    val selectedBytes = remember(uiState, selectedIds) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
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
    val shouldMarkSelectionSeen = remember(uiState, selectedIds) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
        downloads.any { it.id in selectedIds && !it.seen }
    }

    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
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
        modifier = modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DownloadsTopBar(
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                dismissingIds = dismissingIds,
                archivingIds = archivingIds,
                allSelected = allSelected,
                allIds = allIds,
                hasDownloads = hasDownloads,
                hasFailedDownloads = hasFailedDownloads,
                hasActiveDownloads = hasActiveDownloads,
                selectionCountTitle = selectionCountTitle,
                selectionSizeTitle = selectionSizeTitle,
                shouldMarkSelectionSeen = shouldMarkSelectionSeen,
                searchActive = searchActive,
                searchQuery = searchQuery,
                isArchived = isArchived,
                gridLayoutEnabled = (uiState as? DownloadsUiState.Data)
                    ?.data
                    ?.gridLayoutEnabled ?: false,
                lazyGridState = lazyGridState,
                searchFocusRequester = searchFocusRequester,
                onSelectionChange = { selectedIds = it },
                onDismissingIdsChange = { dismissingIds = it },
                onArchivingIdsChange = { archivingIds = it },
                onSearchActiveChange = { searchActive = it },
                onSearchQueryChange = onUpdateSearchQuery,
                onNavigateBack = onNavigateBack,
                onNavigateToArchived = onNavigateToArchived,
                onNavigateToSettings = onNavigateToSettings,
                onToggleDownloadsSeen = onToggleDownloadsSeen,
                onMarkDownloadsPendingDelete = onMarkDownloadsPendingDelete,
                onUpdateDownloadsArchived = onUpdateDownloadsArchived,
                onRetryFailedDownloads = onRetryFailedDownloads,
                onStopAllDownloads = onStopAllDownloads,
                onToggleGridLayout = onToggleGridLayout,
            )
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
                        modifier = Modifier
                            .fillMaxSize(),
                        alignment = Alignment.Center,
                    )
                }

                is DownloadsUiState.Data -> {
                    with(state.data) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            availableUpdate?.let {
                                AvailableUpdateBanner(
                                    modifier = Modifier
                                        .fillMaxWidth(),
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
                                searchQuery = searchQuery.text,
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
                                    when (item.status) {
                                        DownloadStatusUiData.QUEUED,
                                        DownloadStatusUiData.WAITING_FOR_NETWORK,
                                        DownloadStatusUiData.WAITING_FOR_WIFI,
                                        DownloadStatusUiData.DOWNLOADING,
                                        DownloadStatusUiData.METADATA -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onStopDownload(item.id)
                                        }

                                        else -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            onRetryDownload(item.id)
                                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTopBar(
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    dismissingIds: Set<Long>,
    archivingIds: Set<Long>,
    allSelected: Boolean,
    allIds: Set<Long>,
    hasDownloads: Boolean,
    hasFailedDownloads: Boolean,
    hasActiveDownloads: Boolean,
    selectionCountTitle: String,
    selectionSizeTitle: String,
    shouldMarkSelectionSeen: Boolean,
    searchActive: Boolean,
    searchQuery: TextFieldValue,
    isArchived: Boolean,
    gridLayoutEnabled: Boolean,
    lazyGridState: LazyGridState,
    searchFocusRequester: FocusRequester,
    onSelectionChange: (Set<Long>) -> Unit,
    onDismissingIdsChange: (Set<Long>) -> Unit,
    onArchivingIdsChange: (Set<Long>) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleDownloadsSeen: (Set<Long>) -> Unit,
    onMarkDownloadsPendingDelete: (Set<Long>, Boolean) -> Unit,
    onUpdateDownloadsArchived: (Set<Long>, Boolean) -> Unit,
    onRetryFailedDownloads: () -> Unit,
    onStopAllDownloads: () -> Unit,
    onToggleGridLayout: () -> Unit,
) {
    val view = LocalView.current
    val dimens = LocalDimens.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val coroutineScope = rememberCoroutineScope()

    if (selectionMode) {
        TopAppBar(
            colors = RefineryTopAppBarDefaults.colors(),
            navigationIcon = {
                IconButton(
                    onClick = {
                        onSelectionChange(emptySet())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                    )
                }
            },
            title = {
                Column {
                    Text(
                        text = selectionCountTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectionSizeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        onSelectionChange(
                            if (allSelected) {
                                emptySet()
                            } else {
                                allIds
                            }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SelectAll,
                        contentDescription = stringResource(R.string.content_description_select_all),
                    )
                }
                IconButton(
                    onClick = {
                        onToggleDownloadsSeen(selectedIds)
                    },
                ) {
                    Icon(
                        imageVector = if (shouldMarkSelectionSeen) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        },
                        contentDescription = if (shouldMarkSelectionSeen) {
                            stringResource(R.string.content_description_mark_seen)
                        } else {
                            stringResource(R.string.content_description_mark_unseen)
                        },
                    )
                }
                IconButton(
                    onClick = {
                        val visibleSelectedIds = getBulkDeleteVisibleIds(
                            selectedIds = selectedIds,
                            visibleItemKeys = lazyGridState.layoutInfo.visibleItemsInfo.map { it.key },
                        )
                        val hiddenSelectedIds = selectedIds - visibleSelectedIds
                        onArchivingIdsChange(archivingIds + visibleSelectedIds)
                        if (hiddenSelectedIds.isNotEmpty()) {
                            onUpdateDownloadsArchived(hiddenSelectedIds, !isArchived)
                        }
                        onSelectionChange(emptySet())
                    },
                ) {
                    Icon(
                        imageVector = if (isArchived) {
                            Icons.Filled.Unarchive
                        } else {
                            Icons.Filled.Archive
                        },
                        contentDescription = if (isArchived) {
                            stringResource(R.string.content_description_unarchive)
                        } else {
                            stringResource(R.string.content_description_archive)
                        },
                    )
                }
                IconButton(
                    onClick = {
                        val visibleSelectedIds = getBulkDeleteVisibleIds(
                            selectedIds = selectedIds,
                            visibleItemKeys = lazyGridState.layoutInfo.visibleItemsInfo.map { it.key },
                        )
                        val hiddenSelectedIds = selectedIds - visibleSelectedIds
                        onDismissingIdsChange(dismissingIds + visibleSelectedIds)
                        if (hiddenSelectedIds.isNotEmpty()) {
                            onMarkDownloadsPendingDelete(hiddenSelectedIds, true)
                        }
                        onSelectionChange(emptySet())
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_description_delete),
                    )
                }
            },
        )
    } else {
        TopAppBar(
            colors = RefineryTopAppBarDefaults.colors(),
            navigationIcon = {
                if (isArchived) {
                    IconButton(
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                } else {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_appbar),
                            contentDescription = stringResource(R.string.app_name),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AnimatedVisibility(
                        visible = !searchActive,
                        enter = Transitions.titleEnter,
                        exit = Transitions.titleExit,
                    ) {
                        Text(
                            modifier = Modifier
                                .combinedClickable {
                                    coroutineScope.launch {
                                        lazyGridState.animateScrollToItem(0)
                                    }
                                },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyles.downloadsTitle(),
                            text = if (isArchived) {
                                stringResource(R.string.screen_title_archived)
                            } else {
                                stringResource(R.string.app_name)
                            },
                            maxLines = 1,
                        )
                    }
                    AnimatedVisibility(
                        visible = searchActive,
                        enter = Transitions.searchEnter,
                        exit = Transitions.searchExit,
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = dimens.spacingSmall)
                                .focusRequester(searchFocusRequester),
                            singleLine = true,
                            placeholder = {
                                Text(text = stringResource(R.string.hint_search))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = null,
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        onSearchActiveChange(!searchActive)
                        if (searchActive) {
                            onSearchQueryChange(TextFieldValue(""))
                            keyboardController?.hide()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (searchActive) {
                            Icons.Filled.Close
                        } else {
                            Icons.Filled.Search
                        },
                        contentDescription = null,
                    )
                }
                if (!searchActive) {
                    IconButton(
                        onClick = {
                            onToggleGridLayout()
                        },
                    ) {
                        Icon(
                            imageVector = if (gridLayoutEnabled) {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Filled.ViewModule
                            },
                            contentDescription = if (gridLayoutEnabled) {
                                stringResource(R.string.content_description_use_list_layout)
                            } else {
                                stringResource(R.string.content_description_use_grid_layout)
                            },
                        )
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        menuExpanded = !menuExpanded
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    modifier = Modifier
                        .padding(end = dimens.spacingSmall),
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    },
                ) {
                    if (hasDownloads) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.select_all),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SelectAll,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onSelectionChange(allIds)
                                menuExpanded = false
                            },
                        )
                    }
                    if (hasFailedDownloads) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.retry_failed),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.content_description_retry_failed),
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onRetryFailedDownloads()
                                coroutineScope.launch {
                                    delay(RETRY_FAILED_SCROLL_DELAY_MILLIS.milliseconds)
                                    lazyGridState.animateScrollToItem(0)
                                }
                                menuExpanded = false
                            },
                        )
                    }
                    if (hasActiveDownloads) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.notification_action_stop_all),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.notification_action_stop_all),
                                )
                            },
                            onClick = {
                                onStopAllDownloads()
                                menuExpanded = false
                            },
                        )
                    }
                    if (!isArchived) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.screen_title_archived),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Archive,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onNavigateToArchived()
                                menuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.screen_title_settings),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onNavigateToSettings()
                            menuExpanded = false
                        },
                    )
                }
            },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsContent(
    items: List<DownloadItemUiData>,
    selectedIds: Set<Long>,
    dismissingIds: Set<Long>,
    archivingIds: Set<Long>,
    pendingDeleteIds: Set<Long>,
    archived: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    hasAvailableUpdateBanner: Boolean,
    searchQuery: String,
    gridLayoutEnabled: Boolean,
    lazyGridState: LazyGridState,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onBulkDismissAnimationFinished: (Long) -> Unit,
    onBulkArchiveAnimationFinished: (Long) -> Unit,
    onToggleSeen: (Long) -> Unit,
    onMarkPendingDelete: (Set<Long>) -> Unit,
) {
    val dimens = LocalDimens.current

    val coroutineScope = rememberCoroutineScope()
    val itemIds = remember(items) { items.map(DownloadItemUiData::id) }
    val latestItemIds by rememberUpdatedState(itemIds)
    val animatingOutIds = remember { mutableStateMapOf<Long, Boolean>() }
    val swipeActionIds = remember { mutableStateMapOf<Long, DownloadSwipeActionUiData>() }
    val delayedSwipeActionIds = remember { mutableStateMapOf<Long, Boolean>() }
    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyGridState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val atBottom = lastVisibleIndex == layoutInfo.totalItemsCount - 1
            !atBottom && (lazyGridState.firstVisibleItemIndex > 0 || lazyGridState.firstVisibleItemScrollOffset > 0)
        }
    }
    var previousItemIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var previousPendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val restoringItemIds = getRestoredItemIds(
        previousPendingDeleteIds = previousPendingDeleteIds,
        currentItemIds = itemIds,
        currentPendingDeleteIds = pendingDeleteIds,
    )
    val visibleCount = items.size

    LaunchedEffect(itemIds, pendingDeleteIds, searchQuery) {
        if (hasNewVisibleItem(previousItemIds, itemIds, previousPendingDeleteIds, searchQuery)) {
            lazyGridState.scrollToItem(0)
        }
        previousItemIds = itemIds
        previousPendingDeleteIds = pendingDeleteIds
    }
    LaunchedEffect(restoringItemIds, itemIds) {
        if (shouldScrollToTopOnRestore(
                restoredItemIds = restoringItemIds,
                currentItemIds = itemIds,
                firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex,
            )
        ) {
            lazyGridState.scrollToItem(0)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.Center),
            visible = visibleCount == 0,
            enter = Transitions.emptyEnter,
            exit = Transitions.emptyExit,
        ) {
            if (searchQuery.isNotBlank()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize(),
                    icon = Icons.Filled.Search,
                    title = stringResource(
                        R.string.search_no_results_title,
                        searchQuery,
                    ),
                    body = stringResource(R.string.search_no_results_body),
                )
            } else {
                DownloadsIntro(
                    modifier = Modifier
                        .fillMaxSize(),
                    archived = archived,
                )
            }
        }
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            columns = if (gridLayoutEnabled) {
                GridCells.Adaptive(DOWNLOAD_GRID_MIN_CELL_SIZE)
            } else {
                GridCells.Fixed(1)
            },
            state = lazyGridState,
            contentPadding = PaddingValues(
                start = if (gridLayoutEnabled) dimens.spacingSmall else dimens.zero,
                top = if (hasAvailableUpdateBanner) dimens.spacingXSmall else dimens.zero,
                end = if (gridLayoutEnabled) dimens.spacingSmall else dimens.zero,
                bottom = dimens.spacingXSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                dimens.spacingSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (gridLayoutEnabled) dimens.spacingSmall else dimens.spacingXXSmall,
            ),
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                DownloadsListRow(
                    item = item,
                    selectedIds = selectedIds,
                    isRestoring = item.id in restoringItemIds,
                    gridLayoutEnabled = gridLayoutEnabled,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = null,
                            placementSpec = spring(),
                            fadeOutSpec = null,
                        ),
                    onItemClick = onItemClick,
                    onPauseResume = { clickedItem ->
                        if (selectedIds.isNotEmpty()) {
                            onSelectionChange(toggleSelection(selectedIds, clickedItem.id))
                            return@DownloadsListRow
                        }
                        val shouldScrollToTop = shouldScrollToTopOnPauseResume(clickedItem.status)
                        onPauseResume(clickedItem)
                        if (shouldScrollToTop) {
                            coroutineScope.launch {
                                lazyGridState.animateScrollToItem(0)
                            }
                        }
                    },
                    onSelectionChange = onSelectionChange,
                    isPendingDismiss = animatingOutIds[item.id] == true
                            || item.id in dismissingIds
                            || item.id in archivingIds,
                    isInteractionBlocked = item.id in swipeActionIds,
                    skipDismissAnimation = item.id in delayedSwipeActionIds,
                    archived = archived,
                    seen = item.seen,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                    onSwipeActionPerformed = { itemId, action ->
                        if (action == DownloadSwipeActionUiData.SEEN) {
                            onToggleSeen(itemId)
                        } else {
                            swipeActionIds[itemId] = action
                            onSelectionChange(selectedIds - itemId)
                            delayedSwipeActionIds[itemId] = true
                            coroutineScope.launch {
                                val placementDelayMillis = if (gridLayoutEnabled) {
                                    GRID_SWIPE_PLACEMENT_DELAY_MILLIS
                                } else {
                                    LIST_SWIPE_PLACEMENT_DELAY_MILLIS
                                }
                                delay(placementDelayMillis.milliseconds)
                                if (swipeActionIds[itemId] == action) {
                                    if (itemId in latestItemIds) {
                                        animatingOutIds[itemId] = true
                                    } else {
                                        delayedSwipeActionIds.remove(itemId)
                                        swipeActionIds.remove(itemId)
                                        when (action) {
                                            DownloadSwipeActionUiData.DELETE -> {
                                                onMarkPendingDelete(setOf(itemId))
                                            }

                                            DownloadSwipeActionUiData.ARCHIVE -> {
                                                onBulkArchiveAnimationFinished(itemId)
                                            }

                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onDismissAnimationFinished = { itemId ->
                        delayedSwipeActionIds.remove(itemId)
                        val swipeAction = swipeActionIds.remove(itemId)
                        if (
                            animatingOutIds.remove(itemId) == true &&
                            swipeAction == DownloadSwipeActionUiData.DELETE
                        ) {
                            onMarkPendingDelete(setOf(itemId))
                        }
                        if (swipeAction == DownloadSwipeActionUiData.ARCHIVE) {
                            onBulkArchiveAnimationFinished(itemId)
                        }
                        if (itemId in dismissingIds) {
                            onBulkDismissAnimationFinished(itemId)
                        }
                        if (itemId in archivingIds) {
                            onBulkArchiveAnimationFinished(itemId)
                        }
                    },
                )
            }
        }
        if (showScrollToBottom) {
            ScrollToBottomButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                onClick = {
                    coroutineScope.launch {
                        val targetIndex = items.lastIndex
                        if (targetIndex >= 0) {
                            lazyGridState.animateScrollToItem(index = targetIndex)
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsListRow(
    item: DownloadItemUiData,
    selectedIds: Set<Long>,
    isPendingDismiss: Boolean,
    isInteractionBlocked: Boolean,
    skipDismissAnimation: Boolean,
    isRestoring: Boolean,
    gridLayoutEnabled: Boolean,
    archived: Boolean,
    seen: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    modifier: Modifier = Modifier,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onSwipeActionPerformed: (Long, DownloadSwipeActionUiData) -> Unit,
    onDismissAnimationFinished: (Long) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionEnabled = !isPendingDismiss && !isInteractionBlocked
    val swipeEnabled = !isPendingDismiss && selectedIds.isEmpty()
            && (!isInteractionBlocked || gridLayoutEnabled)
            && (leftSwipeAction != DownloadSwipeActionUiData.NONE || rightSwipeAction != DownloadSwipeActionUiData.NONE)

    val longClickEnabled = interactionEnabled && selectedIds.isEmpty()
    val isSelected = selectedIds.contains(item.id)

    var currentSwipeThresholdRatio by remember(
        item.id,
        leftSwipeAction,
        rightSwipeAction,
    ) {
        mutableFloatStateOf(DISMISS_SWIPE_THRESHOLD_RATIO)
    }
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var hasHandledCurrentSwipe by remember(item.id, seen, leftSwipeAction, rightSwipeAction) {
        mutableStateOf(false)
    }
    var pendingSnapBackSwipeAction by remember(item.id, seen, leftSwipeAction, rightSwipeAction) {
        mutableStateOf<DownloadSwipeActionUiData?>(null)
    }
    var gridSwipeActive by remember(item.id) { mutableStateOf(false) }
    val dismissState = key(item.id, seen, leftSwipeAction, rightSwipeAction) {
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance ->
                totalDistance * currentSwipeThresholdRatio
            },
        )
    }
    val visibilityState = remember(item.id) {
        MutableTransitionState(!isRestoring)
    }

    LaunchedEffect(isPendingDismiss, isRestoring, swipeEnabled) {
        visibilityState.targetState = !isPendingDismiss
        if (isRestoring) {
            visibilityState.targetState = true
        }
        if (!isPendingDismiss && !isInteractionBlocked) {
            runCatching {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }
    }
    LaunchedEffect(
        item.id,
        isPendingDismiss,
        visibilityState.currentState,
        visibilityState.targetState,
    ) {
        if (isPendingDismiss && !visibilityState.currentState && !visibilityState.targetState) {
            onDismissAnimationFinished(item.id)
        }
    }
    LaunchedEffect(isPendingDismiss) {
        if (!isPendingDismiss) {
            hasHandledCurrentSwipe = false
        }
    }
    LaunchedEffect(dismissState, leftSwipeAction, rightSwipeAction) {
        snapshotFlow { runCatching { dismissState.requireOffset() }.getOrDefault(0f) }
            .distinctUntilChanged()
            .collectLatest { offsetPx ->
                currentSwipeThresholdRatio = getSwipeThresholdRatio(
                    offsetPx = offsetPx,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                )
            }
    }

    AnimatedVisibility(
        modifier = modifier
            .zIndex(if (gridSwipeActive) 1f else 0f),
        visibleState = visibilityState,
        enter = Transitions.listItemEnter,
        exit = if (skipDismissAnimation) {
            ExitTransition.None
        } else {
            Transitions.listItemExit
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    rowWidthPx = coordinates.size.width.toFloat()
                },
        ) {
            val onClick = {
                if (selectedIds.isNotEmpty()) {
                    onSelectionChange(toggleSelection(selectedIds, item.id))
                } else {
                    onItemClick(item)
                }
            }
            val onOpen = {
                if (selectedIds.isNotEmpty()) {
                    onSelectionChange(toggleSelection(selectedIds, item.id))
                } else {
                    onItemClick(item)
                }
            }
            val itemContent: @Composable () -> Unit = {
                if (gridLayoutEnabled) {
                    DownloadGridItem(
                        item = item,
                        isSelected = isSelected,
                        interactionEnabled = interactionEnabled,
                        longClickEnabled = longClickEnabled,
                        onClick = onClick,
                        onLongClick = {
                            onSelectionChange(selectedIds + item.id)
                        },
                        onPauseResume = {
                            onPauseResume(item)
                        },
                        onOpen = onOpen,
                    )
                } else {
                    DownloadItem(
                        item = item,
                        isSelected = isSelected,
                        interactionEnabled = interactionEnabled,
                        longClickEnabled = longClickEnabled,
                        onClick = onClick,
                        onLongClick = {
                            onSelectionChange(selectedIds + item.id)
                        },
                        onPauseResume = {
                            onPauseResume(item)
                        },
                        onOpen = onOpen,
                    )
                }
            }
            if (gridLayoutEnabled) {
                GridSwipeToDismiss(
                    enabled = swipeEnabled,
                    archived = archived,
                    seen = seen,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                    onSwipeActionPerformed = { action ->
                        onSwipeActionPerformed(item.id, action)
                    },
                    onSwipeActiveChange = { gridSwipeActive = it },
                    content = { itemContent() },
                )
            } else {
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = swipeEnabled
                            && rightSwipeAction != DownloadSwipeActionUiData.NONE,
                    enableDismissFromEndToStart = swipeEnabled
                            && leftSwipeAction != DownloadSwipeActionUiData.NONE,
                    onDismiss = { dismissValue ->
                        if (hasHandledCurrentSwipe) {
                            return@SwipeToDismissBox
                        }
                        val swipeAction = getSwipeActionForDismissValue(
                            dismissValue = dismissValue,
                            leftSwipeAction = leftSwipeAction,
                            rightSwipeAction = rightSwipeAction,
                        )
                        if (swipeAction == DownloadSwipeActionUiData.NONE) {
                            return@SwipeToDismissBox
                        }
                        if (isSnapBackSwipeAction(swipeAction)) {
                            hasHandledCurrentSwipe = true
                            pendingSnapBackSwipeAction = swipeAction
                            coroutineScope.launch {
                                runCatching {
                                    dismissState.reset()
                                }
                            }
                            return@SwipeToDismissBox
                        }
                        hasHandledCurrentSwipe = true
                        onSwipeActionPerformed(item.id, swipeAction)
                    },
                    backgroundContent = {
                        SwipeActionBackground(
                            archived = archived,
                            seen = seen,
                            leftSwipeAction = leftSwipeAction,
                            rightSwipeAction = rightSwipeAction,
                            offsetPx = runCatching {
                                dismissState.requireOffset()
                            }.getOrDefault(0f),
                        )
                    },
                    content = { itemContent() },
                )
            }
        }
    }

    LaunchedEffect(dismissState, leftSwipeAction, rightSwipeAction) {
        snapshotFlow {
            Pair(
                dismissState.currentValue,
                runCatching { dismissState.requireOffset() }.getOrDefault(0f),
            )
        }
            .distinctUntilChanged()
            .collectLatest { (value, offsetPx) ->
                if (value == SwipeToDismissBoxValue.Settled && abs(offsetPx) < 1f) {
                    pendingSnapBackSwipeAction?.let { swipeAction ->
                        pendingSnapBackSwipeAction = null
                        onSwipeActionPerformed(item.id, swipeAction)
                    }
                    hasHandledCurrentSwipe = false
                }
            }
    }
    LaunchedEffect(dismissState, rowWidthPx, leftSwipeAction, rightSwipeAction) {
        snapshotFlow { runCatching { dismissState.requireOffset() }.getOrDefault(0f) }
            .distinctUntilChanged()
            .collectLatest { offsetPx ->
                if (rowWidthPx <= 0f || hasHandledCurrentSwipe || pendingSnapBackSwipeAction != null) {
                    return@collectLatest
                }
                val swipeAction = when {
                    offsetPx > 0f -> rightSwipeAction
                    offsetPx < 0f -> leftSwipeAction
                    else -> DownloadSwipeActionUiData.NONE
                }
                if (!isSnapBackSwipeAction(swipeAction)) {
                    return@collectLatest
                }
                val triggerDistancePx = rowWidthPx * SNAP_BACK_SWIPE_THRESHOLD_RATIO
                if (abs(offsetPx) < triggerDistancePx) {
                    return@collectLatest
                }
                hasHandledCurrentSwipe = true
                pendingSnapBackSwipeAction = swipeAction
                coroutineScope.launch {
                    runCatching {
                        dismissState.reset()
                    }
                }
            }
    }
}

@Composable
private fun GridSwipeToDismiss(
    enabled: Boolean,
    archived: Boolean,
    seen: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    onSwipeActionPerformed: (DownloadSwipeActionUiData) -> Unit,
    onSwipeActiveChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val coroutineScope = rememberCoroutineScope()
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var cardLeftPx by remember { mutableFloatStateOf(0f) }
    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var handlingSwipe by remember { mutableStateOf(false) }

    suspend fun animateOffsetTo(targetOffsetPx: Float) {
        animate(
            initialValue = offsetPx,
            targetValue = targetOffsetPx,
            animationSpec = tween(GRID_SWIPE_ANIMATION_MILLIS),
        ) { value, _ ->
            offsetPx = value
        }
    }

    fun swipeActionForPhysicalOffset(physicalOffsetPx: Float): DownloadSwipeActionUiData = when {
        physicalOffsetPx == 0f -> DownloadSwipeActionUiData.NONE
        (physicalOffsetPx > 0f) == (layoutDirection == LayoutDirection.Ltr) -> rightSwipeAction
        else -> leftSwipeAction
    }

    fun logicalOffset(physicalOffsetPx: Float): Float = when (layoutDirection) {
        LayoutDirection.Ltr -> physicalOffsetPx
        LayoutDirection.Rtl -> -physicalOffsetPx
    }

    val positiveSwipeAction = swipeActionForPhysicalOffset(1f)
    val negativeSwipeAction = swipeActionForPhysicalOffset(-1f)
    val velocityThresholdPx = with(density) {
        GRID_SWIPE_VELOCITY_THRESHOLD.toPx()
    }

    fun swipeActionForOffset(): DownloadSwipeActionUiData = when {
        offsetPx > 0f -> positiveSwipeAction
        offsetPx < 0f -> negativeSwipeAction
        else -> DownloadSwipeActionUiData.NONE
    }

    val draggableState = rememberDraggableState { deltaPx ->
        if (handlingSwipe || cardWidthPx <= 0f) {
            return@rememberDraggableState
        }
        onSwipeActiveChange(true)
        val viewportWidthPx = view.width.toFloat().coerceAtLeast(cardWidthPx)
        val maximumStartToEndOffsetPx = (viewportWidthPx - cardLeftPx)
            .coerceAtLeast(cardWidthPx)
        val maximumEndToStartOffsetPx = (cardLeftPx + cardWidthPx)
            .coerceAtLeast(cardWidthPx)
        val minimumOffsetPx = if (negativeSwipeAction == DownloadSwipeActionUiData.NONE) {
            0f
        } else {
            -maximumEndToStartOffsetPx
        }
        val maximumOffsetPx = if (positiveSwipeAction == DownloadSwipeActionUiData.NONE) {
            0f
        } else {
            maximumStartToEndOffsetPx
        }
        offsetPx = (offsetPx + deltaPx).coerceIn(minimumOffsetPx, maximumOffsetPx)

        val swipeAction = swipeActionForOffset()
        if (isSnapBackSwipeAction(swipeAction)
            && abs(offsetPx) >= cardWidthPx * SNAP_BACK_SWIPE_THRESHOLD_RATIO
        ) {
            handlingSwipe = true
            onSwipeActionPerformed(swipeAction)
            coroutineScope.launch {
                animateOffsetTo(0f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardLeftPx = coordinates.positionInWindow().x
                cardWidthPx = coordinates.size.width.toFloat()
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                onDragStopped = { velocityPxPerSecond ->
                    if (handlingSwipe) {
                        handlingSwipe = false
                        onSwipeActiveChange(false)
                        return@draggable
                    }
                    if (cardWidthPx <= 0f) {
                        return@draggable
                    }
                    val fastSwipe = abs(velocityPxPerSecond) >= velocityThresholdPx
                    val swipeAction = if (fastSwipe) {
                        swipeActionForPhysicalOffset(velocityPxPerSecond)
                    } else {
                        swipeActionForOffset()
                    }
                    if (swipeAction == DownloadSwipeActionUiData.NONE) {
                        animateOffsetTo(0f)
                        onSwipeActiveChange(false)
                        return@draggable
                    }
                    if (isSnapBackSwipeAction(swipeAction)) {
                        if (fastSwipe) {
                            handlingSwipe = true
                            onSwipeActionPerformed(swipeAction)
                        }
                        animateOffsetTo(0f)
                        handlingSwipe = false
                        onSwipeActiveChange(false)
                        return@draggable
                    }
                    if (!fastSwipe && abs(offsetPx) < cardWidthPx * DISMISS_SWIPE_THRESHOLD_RATIO) {
                        animateOffsetTo(0f)
                        onSwipeActiveChange(false)
                        return@draggable
                    }

                    handlingSwipe = true
                    onSwipeActionPerformed(swipeAction)
                    val viewportWidthPx = view.width.toFloat().coerceAtLeast(cardWidthPx)
                    val dismissesTowardEnd = if (fastSwipe) {
                        velocityPxPerSecond > 0f
                    } else {
                        offsetPx > 0f
                    }
                    val targetOffsetPx = if (dismissesTowardEnd) {
                        (viewportWidthPx - cardLeftPx).coerceAtLeast(cardWidthPx)
                    } else {
                        -(cardLeftPx + cardWidthPx).coerceAtLeast(cardWidthPx)
                    }
                    animateOffsetTo(targetOffsetPx)
                },
            ),
    ) {
        SwipeActionBackground(
            modifier = Modifier
                .matchParentSize(),
            archived = archived,
            seen = seen,
            leftSwipeAction = leftSwipeAction,
            rightSwipeAction = rightSwipeAction,
            offsetPx = logicalOffset(offsetPx),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) },
        ) {
            content()
        }
    }
}

private fun isSnapBackSwipeAction(
    swipeAction: DownloadSwipeActionUiData,
): Boolean {
    return swipeAction == DownloadSwipeActionUiData.SEEN
}

private fun getSwipeThresholdRatio(
    offsetPx: Float,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
): Float {
    val swipeAction = when {
        offsetPx > 0f -> rightSwipeAction
        offsetPx < 0f -> leftSwipeAction
        else -> DownloadSwipeActionUiData.NONE
    }
    return if (isSnapBackSwipeAction(swipeAction)) {
        SNAP_BACK_SWIPE_THRESHOLD_RATIO
    } else {
        DISMISS_SWIPE_THRESHOLD_RATIO
    }
}

@Composable
private fun SwipeActionBackground(
    archived: Boolean,
    seen: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    offsetPx: Float,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    val swipeAction = when {
        offsetPx > 0f -> rightSwipeAction
        offsetPx < 0f -> leftSwipeAction
        else -> DownloadSwipeActionUiData.NONE
    }
    val isEndToStart = offsetPx < 0f
    val containerColor = when (swipeAction) {
        DownloadSwipeActionUiData.DELETE -> MaterialTheme.colorScheme.errorContainer
        DownloadSwipeActionUiData.ARCHIVE -> MaterialTheme.colorScheme.secondaryContainer
        DownloadSwipeActionUiData.SEEN -> MaterialTheme.colorScheme.tertiaryContainer
        DownloadSwipeActionUiData.NONE -> Color.Transparent
    }
    val contentColor = when (swipeAction) {
        DownloadSwipeActionUiData.DELETE -> MaterialTheme.colorScheme.onErrorContainer
        DownloadSwipeActionUiData.ARCHIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        DownloadSwipeActionUiData.SEEN -> MaterialTheme.colorScheme.onTertiaryContainer
        DownloadSwipeActionUiData.NONE -> Color.Transparent
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = if (isEndToStart) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            },
        ) {
            Icon(
                modifier = Modifier
                    .padding(
                        start = if (isEndToStart) dimens.zero else dimens.spacingMedium,
                        end = if (isEndToStart) dimens.spacingMedium else dimens.zero,
                    ),
                imageVector = getSwipeActionIcon(
                    action = swipeAction,
                    archived = archived,
                    seen = seen,
                ),
                tint = contentColor,
                contentDescription = null,
            )
        }
    }
}

private fun getSwipeActionIcon(
    action: DownloadSwipeActionUiData,
    archived: Boolean,
    seen: Boolean,
): ImageVector {
    return when (action) {
        DownloadSwipeActionUiData.NONE -> Icons.Filled.MoreVert
        DownloadSwipeActionUiData.ARCHIVE -> {
            if (archived) {
                Icons.Filled.Unarchive
            } else {
                Icons.Filled.Archive
            }
        }

        DownloadSwipeActionUiData.SEEN -> {
            if (seen) {
                Icons.Filled.VisibilityOff
            } else {
                Icons.Filled.Visibility
            }
        }

        DownloadSwipeActionUiData.DELETE -> Icons.Filled.Delete
    }
}

private fun getSwipeActionForDismissValue(
    dismissValue: SwipeToDismissBoxValue,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
): DownloadSwipeActionUiData {
    return when (dismissValue) {
        StartToEnd -> rightSwipeAction
        EndToStart -> leftSwipeAction
        SwipeToDismissBoxValue.Settled -> DownloadSwipeActionUiData.NONE
    }
}

@Composable
private fun ScrollToBottomButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    IconButton(
        modifier = Modifier
            .then(modifier)
            .padding(dimens.spacingMedium),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(dimens.overlayButton)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(dimens.overlayButton / 2),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier
                    .size(dimens.iconXSmall),
                imageVector = Icons.Default.ArrowDownward,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun DownloadGridItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
    interactionEnabled: Boolean,
    longClickEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val dimens = LocalDimens.current
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = dimens.tonalElevationLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = interactionEnabled,
                    onClick = onClick,
                    onLongClick = if (longClickEnabled) {
                        onLongClick
                    } else {
                        null
                    },
                ),
        ) {
            DownloadPrimaryActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                thumbnailFilePath = item.thumbnailFilePath,
                status = item.status,
                enabled = interactionEnabled,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            Column(
                modifier = Modifier
                    .padding(dimens.spacingSmall),
            ) {
                DownloadItemTitle(item = item)
                Spacer(modifier = Modifier.height(dimens.spacingSmall))
                DownloadProgressSection(
                    status = item.status,
                    progressFraction = item.progressFraction,
                    etaSeconds = item.etaSeconds,
                    bytesDownloaded = item.bytesDownloaded,
                    completedAtMillis = item.completedAtMillis,
                    alwaysShowBar = false,
                )
            }
        }
    }
}

@Composable
private fun DownloadItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
    interactionEnabled: Boolean,
    longClickEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val dimens = LocalDimens.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.background
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = interactionEnabled,
                    onClick = onClick,
                    onLongClick = if (longClickEnabled) {
                        onLongClick
                    } else {
                        null
                    },
                )
                .padding(
                    vertical = dimens.spacingSmall,
                    horizontal = dimens.spacingMedium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadPrimaryActionButton(
                modifier = Modifier
                    .size(dimens.downloadListThumbnailSize),
                thumbnailFilePath = item.thumbnailFilePath,
                status = item.status,
                enabled = interactionEnabled,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            Spacer(modifier = Modifier.width(dimens.spacingMediumAlt))
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
                val showInlineProgressBar = when (item.status) {
                    DownloadStatusUiData.QUEUED,
                    DownloadStatusUiData.METADATA,
                    DownloadStatusUiData.DOWNLOADING -> true

                    else -> false
                }
                DownloadItemTitle(item = item)
                Spacer(
                    modifier = Modifier.height(
                        if (showInlineProgressBar) {
                            dimens.spacingMediumAlt
                        } else {
                            dimens.spacingXXSmall
                        },
                    ),
                )
                DownloadProgressSection(
                    status = item.status,
                    progressFraction = item.progressFraction,
                    etaSeconds = item.etaSeconds,
                    bytesDownloaded = item.bytesDownloaded,
                    completedAtMillis = item.completedAtMillis,
                    alwaysShowBar = false,
                )
            }
        }
    }
}

@Composable
private fun DownloadItemTitle(
    item: DownloadItemUiData,
) {
    val dimens = LocalDimens.current

    val showUnseen = !item.seen && item.status == DownloadStatusUiData.COMPLETED
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (showUnseen) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            text = item.title,
        )
        if (showUnseen) {
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            Box(
                modifier = Modifier
                    .size(dimens.dotSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

@Composable
private fun DownloadsIntro(
    modifier: Modifier = Modifier,
    archived: Boolean = false,
) {
    val appName = stringResource(R.string.app_name)
    EmptyState(
        modifier = modifier,
        icon = if (archived) {
            Icons.Filled.Archive
        } else {
            ImageVector.vectorResource(id = R.drawable.ic_logo)
        },
        title = if (archived) {
            stringResource(R.string.archived_intro_title)
        } else {
            stringResource(R.string.downloads_intro_title, appName)
        },
        body = if (archived) {
            stringResource(R.string.archived_intro_body)
        } else {
            stringResource(R.string.downloads_intro_body, appName)
        },
    )
}

@Composable
private fun DownloadsError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier
            .fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_downloads),
    )
}

private fun shouldScrollToTopOnPauseResume(status: DownloadStatusUiData): Boolean = when (status) {
    DownloadStatusUiData.QUEUED,
    DownloadStatusUiData.WAITING_FOR_NETWORK,
    DownloadStatusUiData.WAITING_FOR_WIFI,
    DownloadStatusUiData.DOWNLOADING,
    DownloadStatusUiData.METADATA -> false

    else -> true
}

private fun toggleSelection(selectedIds: Set<Long>, itemId: Long): Set<Long> {
    return if (itemId in selectedIds) {
        selectedIds - itemId
    } else {
        selectedIds + itemId
    }
}

internal fun getBulkDeleteVisibleIds(
    selectedIds: Set<Long>,
    visibleItemKeys: List<Any>,
): Set<Long> {
    if (selectedIds.isEmpty() || visibleItemKeys.isEmpty()) {
        return emptySet()
    }
    val visibleIds = visibleItemKeys
        .filterIsInstance<Long>()
        .toSet()

    return selectedIds.intersect(visibleIds)
}

internal fun hasNewVisibleItem(
    previousItemIds: List<Long>,
    currentItemIds: List<Long>,
    previousPendingDeleteIds: Set<Long>,
    searchQuery: String,
): Boolean {
    if (searchQuery.isNotBlank()) {
        return false
    }
    if (previousItemIds.isEmpty() || currentItemIds.isEmpty()) {
        return false
    }
    val previousIdsSet = previousItemIds.toSet()
    return currentItemIds.any { it !in previousIdsSet && it !in previousPendingDeleteIds }
}

internal fun getRestoredItemIds(
    previousPendingDeleteIds: Set<Long>,
    currentItemIds: List<Long>,
    currentPendingDeleteIds: Set<Long>,
): Set<Long> {
    if (previousPendingDeleteIds.isEmpty()) {
        return emptySet()
    }
    return previousPendingDeleteIds
        .intersect(currentItemIds.toSet())
        .minus(currentPendingDeleteIds)
}

internal fun shouldScrollToTopOnRestore(
    restoredItemIds: Set<Long>,
    currentItemIds: List<Long>,
    firstVisibleItemIndex: Int,
): Boolean {
    if (restoredItemIds.isEmpty() || currentItemIds.isEmpty()) {
        return false
    }
    val userWasNearTop = firstVisibleItemIndex <= 1
    return userWasNearTop && currentItemIds.first() in restoredItemIds
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
