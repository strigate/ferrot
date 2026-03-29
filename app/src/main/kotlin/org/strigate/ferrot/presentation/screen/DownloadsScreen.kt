package org.strigate.ferrot.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
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
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.theme.TextStyles
import org.strigate.ferrot.presentation.transitions.Transitions
import org.strigate.ferrot.presentation.util.LifecycleEffect
import org.strigate.ferrot.presentation.viewmodel.DownloadsViewModel
import kotlin.math.abs

private const val SEARCH_FOCUS_DELAY_MILLIS = 357L
private const val RETRY_FAILED_SCROLL_DELAY_MILLIS = 357L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var dismissingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var snackbarUndoDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    val snackbarSingleDeleteMessage = stringResource(R.string.snackbar_delete_single_delete)
    val snackbarBulkDeleteMessage = stringResource(R.string.snackbar_bulk_delete_bulk_delete)

    BackHandler(enabled = searchActive) {
        searchActive = false
        viewModel.updateSearchQuery(TextFieldValue(""))
        keyboardController?.hide()
    }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(SEARCH_FOCUS_DELAY_MILLIS)
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
    val pendingDeleteIds = (uiState as? DownloadsUiState.Data)?.data?.pendingDeleteIds ?: emptySet()
    val hasPendingDeletes = pendingDeleteIds.isNotEmpty()

    val hasFailedDownloads = remember(uiState) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
        downloads.any { it.status == DownloadStatusUiData.FAILED }
    }
    val shouldMarkSelectionSeen = remember(uiState, selectedIds) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
        downloads.any { it.id in selectedIds && !it.seen }
    }

    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }
    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LifecycleEffect {
        on(Lifecycle.Event.ON_START) {
            viewModel.requestDeletePendingDownloadsImmediate()
        }
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
            viewModel.markDownloadsPendingDelete(ids, pendingDelete = false)
        },
        onConfirmPendingDelete = viewModel::requestDeletePendingDownloadsImmediate,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                            )
                        }
                    },
                    title = {
                        Text("${selectedIds.size} ${stringResource(R.string.bulk_selected)}")
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    allIds
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = stringResource(R.string.content_description_select_all),
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.toggleDownloadsSeen(selectedIds)
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
                                    visibleItemKeys = lazyListState.layoutInfo.visibleItemsInfo.map { it.key },
                                )
                                val hiddenSelectedIds = selectedIds - visibleSelectedIds
                                dismissingIds = dismissingIds + visibleSelectedIds
                                if (hiddenSelectedIds.isNotEmpty()) {
                                    viewModel.markDownloadsPendingDelete(hiddenSelectedIds)
                                }
                                selectedIds = emptySet()
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
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier
                                .padding(dimens.spacingXSmall),
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_appbar),
                                contentDescription = stringResource(R.string.app_name),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    title = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TopAppBarDefaults.TopAppBarExpandedHeight),
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
                                                lazyListState.animateScrollToItem(0)
                                            }
                                        },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = TextStyles.downloadsTitle(),
                                    text = stringResource(R.string.app_name),
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
                                    onValueChange = viewModel::updateSearchQuery,
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
                                searchActive = !searchActive
                                if (!searchActive) {
                                    viewModel.updateSearchQuery(TextFieldValue(""))
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
                                        selectedIds = allIds
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
                                        viewModel.retryFailedDownloads()
                                        coroutineScope.launch {
                                            delay(RETRY_FAILED_SCROLL_DELAY_MILLIS)
                                            lazyListState.animateScrollToItem(0)
                                        }
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
                                    navController.navigate(Screen.Settings.route)
                                    menuExpanded = false
                                },
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
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
                                val context = LocalContext.current
                                AvailableUpdateBanner(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = dimens.spacingMedium)
                                        .padding(bottom = dimens.spacingSmall),
                                    tag = it.tag,
                                    localFilePath = it.localFilePath,
                                    onClick = { filePath ->
                                        InstallHelper.requestInstallApkIfExists(context, filePath)
                                    },
                                )
                            }
                            DownloadsList(
                                items = downloads,
                                selectedIds = selectedIds,
                                dismissingIds = dismissingIds,
                                pendingDeleteIds = pendingDeleteIds,
                                searchQuery = searchQuery.text,
                                lazyListState = lazyListState,
                                onItemClick = { item ->
                                    if (pendingDeleteIds.isNotEmpty()) {
                                        viewModel.requestDeletePendingDownloadsImmediate()
                                    }
                                    keyboardController?.hide()
                                    navController.navigate(Screen.Download.route(item.id))
                                },
                                onPauseResume = { item ->
                                    when (item.status) {
                                        DownloadStatusUiData.QUEUED,
                                        DownloadStatusUiData.WAITING_FOR_NETWORK,
                                        DownloadStatusUiData.WAITING_FOR_WIFI,
                                        DownloadStatusUiData.DOWNLOADING,
                                        DownloadStatusUiData.METADATA -> {
                                            viewModel.stopDownload(item.id)
                                        }

                                        else -> {
                                            viewModel.retryDownload(item.id)
                                        }
                                    }
                                },
                                onSelectionChange = {
                                    selectedIds = it
                                },
                                onBulkDismissAnimationFinished = { itemId ->
                                    dismissingIds = dismissingIds - itemId
                                    viewModel.markDownloadsPendingDelete(setOf(itemId))
                                },
                                onMarkPendingDelete = viewModel::markDownloadsPendingDelete,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsList(
    items: List<DownloadItemUiData>,
    selectedIds: Set<Long>,
    dismissingIds: Set<Long>,
    pendingDeleteIds: Set<Long>,
    searchQuery: String,
    lazyListState: LazyListState,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onBulkDismissAnimationFinished: (Long) -> Unit,
    onMarkPendingDelete: (Set<Long>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val itemIds = remember(items) { items.map(DownloadItemUiData::id) }
    val animatingOutIds = remember { mutableStateMapOf<Long, Boolean>() }

    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val atBottom = lastVisibleIndex == layoutInfo.totalItemsCount - 1
            !atBottom && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
        }
    }
    var previousItemIds by remember {
        mutableStateOf<List<Long>>(emptyList())
    }
    var previousPendingDeleteIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    val restoringItemIds = getRestoredItemIds(
        previousPendingDeleteIds = previousPendingDeleteIds,
        currentItemIds = itemIds,
        currentPendingDeleteIds = pendingDeleteIds,
    )
    val visibleCount by remember(items) {
        derivedStateOf {
            items.size
        }
    }
    LaunchedEffect(itemIds, pendingDeleteIds, searchQuery) {
        if (hasNewItemAtTop(previousItemIds, itemIds, previousPendingDeleteIds, searchQuery)) {
            lazyListState.scrollToItem(0)
        }
        previousItemIds = itemIds
        previousPendingDeleteIds = pendingDeleteIds
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
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                DownloadsListRow(
                    item = item,
                    selectedIds = selectedIds,
                    isRestoring = item.id in restoringItemIds,
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
                                lazyListState.animateScrollToItem(0)
                            }
                        }
                    },
                    onSelectionChange = onSelectionChange,
                    isPendingDismiss = animatingOutIds[item.id] == true || item.id in dismissingIds,
                    onSwipeActionPerformed = { itemId ->
                        animatingOutIds[itemId] = true
                        onSelectionChange(selectedIds - itemId)
                    },
                    onDismissAnimationFinished = { itemId ->
                        if (animatingOutIds.remove(itemId) == true) {
                            onMarkPendingDelete(setOf(itemId))
                        }
                        if (itemId in dismissingIds) {
                            onBulkDismissAnimationFinished(itemId)
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
                            lazyListState.animateScrollToItem(index = targetIndex)
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
    isRestoring: Boolean,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onSwipeActionPerformed: (Long) -> Unit,
    onDismissAnimationFinished: (Long) -> Unit,
) {
    val dimens = LocalDimens.current

    val swipeEnabled = selectedIds.isEmpty()
    val isSelected = selectedIds.contains(item.id)
    val dismissState = rememberSwipeToDismissBoxState()
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val visibilityState = remember(item.id) {
        MutableTransitionState(!isRestoring)
    }

    LaunchedEffect(isPendingDismiss, isRestoring, swipeEnabled) {
        visibilityState.targetState = !isPendingDismiss
        if (isRestoring) {
            visibilityState.targetState = true
        }
        if (!isPendingDismiss) {
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

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = Transitions.listItemEnter,
        exit = Transitions.listItemExit,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        rowWidthPx = coordinates.size.width.toFloat()
                    },
            ) {
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = swipeEnabled,
                    backgroundContent = {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .padding(end = dimens.spacingMedium),
                                    imageVector = Icons.Filled.Delete,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                ) {
                    DownloadItem(
                        item = item,
                        isSelected = isSelected,
                        longClickEnabled = swipeEnabled,
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                onSelectionChange(toggleSelection(selectedIds, item.id))
                            } else {
                                onItemClick(item)
                            }
                        },
                        onLongClick = {
                            onSelectionChange(selectedIds + item.id)
                        },
                        onPauseResume = {
                            onPauseResume(item)
                        },
                        onOpen = {
                            if (selectedIds.isNotEmpty()) {
                                onSelectionChange(toggleSelection(selectedIds, item.id))
                            } else {
                                onItemClick(item)
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimens.spacingXXSmall))
        }
    }

    LaunchedEffect(dismissState) {
        snapshotFlow {
            Pair(
                dismissState.currentValue,
                runCatching { dismissState.requireOffset() }.getOrDefault(0f),
            )
        }
            .distinctUntilChanged()
            .collectLatest { (value, offsetPx) ->
                val fullyAtEnd = value == SwipeToDismissBoxValue.EndToStart &&
                        rowWidthPx > 0f &&
                        abs(offsetPx) >= (rowWidthPx - 1f)
                if (fullyAtEnd) {
                    onSwipeActionPerformed(item.id)
                }
            }
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
private fun DownloadItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
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
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = true,
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
                thumbnailFilePath = item.thumbnailFilePath,
                status = item.status,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            Spacer(modifier = Modifier.width(dimens.spacingMediumAlt))
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
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
                Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))
                DownloadProgressSection(
                    status = item.status,
                    progressFraction = item.progressFraction,
                    etaSeconds = item.etaSeconds,
                    bytesDownloaded = item.bytesDownloaded,
                    completedAtMillis = item.completedAtMillis,
                )
            }
        }
    }
}

@Composable
private fun DownloadsIntro(
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(R.string.app_name)
    EmptyState(
        modifier = modifier,
        icon = ImageVector.vectorResource(id = R.drawable.ic_logo),
        title = stringResource(R.string.downloads_intro_title, appName),
        body = stringResource(R.string.downloads_intro_body, appName),
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
        .mapNotNull { it as? Long }
        .toSet()

    return selectedIds.intersect(visibleIds)
}

internal fun hasNewItemAtTop(
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
