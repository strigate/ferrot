package org.strigate.ferrot.presentation.screen

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.model.isFailed
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.presentation.theme.FerrotTopAppBarDefaults
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.theme.TextStyles
import org.strigate.ferrot.presentation.transitions.Transitions
import org.strigate.ferrot.presentation.util.LifecycleEffect
import org.strigate.ferrot.presentation.viewmodel.DownloadsViewModel
import kotlin.math.abs

private const val SEARCH_FOCUS_DELAY_MILLIS = 357L
private const val RETRY_FAILED_SCROLL_DELAY_MILLIS = 357L
private const val SNAP_BACK_SWIPE_THRESHOLD_RATIO = 0.3f
private const val DISMISS_SWIPE_THRESHOLD_RATIO = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    navController: NavController,
    archived: Boolean,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isArchived by viewModel.isArchived.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    var searchActive by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var dismissingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var archivingIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
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
        downloads.any { it.status.isFailed }
    }
    val hasActiveDownloads = remember(uiState) {
        val downloads = (uiState as? DownloadsUiState.Data)?.data?.downloads.orEmpty()
        downloads.any { it.status.isActive }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    colors = FerrotTopAppBarDefaults.colors(),
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
                                archivingIds = archivingIds + visibleSelectedIds
                                if (hiddenSelectedIds.isNotEmpty()) {
                                    viewModel.updateDownloadsArchived(
                                        downloadIds = hiddenSelectedIds,
                                        archived = !isArchived,
                                    )
                                }
                                selectedIds = emptySet()
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
                    colors = FerrotTopAppBarDefaults.colors(),
                    navigationIcon = {
                        if (isArchived) {
                            IconButton(
                                onClick = {
                                    navController.navigateUp()
                                },
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
                                                lazyListState.animateScrollToItem(0)
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
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        viewModel.retryFailedDownloads()
                                        coroutineScope.launch {
                                            delay(RETRY_FAILED_SCROLL_DELAY_MILLIS)
                                            lazyListState.animateScrollToItem(0)
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
                                        viewModel.stopAllDownloads()
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
                                        navController.navigate(Screen.Archived.route)
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
                                    onClick = {
                                        viewModel.installAvailableUpdate()
                                    },
                                )
                            }
                            DownloadsList(
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
                                lazyListState = lazyListState,
                                onItemClick = { item ->
                                    if (pendingDeleteIds.isNotEmpty()) {
                                        viewModel.requestDeletePendingDownloadsImmediate()
                                    }
                                    keyboardController?.hide()
                                    navController.navigate(
                                        Screen.Download.route(
                                            id = item.id,
                                            archived = isArchived,
                                        )
                                    )
                                },
                                onPauseResume = { item ->
                                    when (item.status) {
                                        DownloadStatusUiData.QUEUED,
                                        DownloadStatusUiData.WAITING_FOR_NETWORK,
                                        DownloadStatusUiData.WAITING_FOR_WIFI,
                                        DownloadStatusUiData.DOWNLOADING,
                                        DownloadStatusUiData.METADATA -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            viewModel.stopDownload(item.id)
                                        }

                                        else -> {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
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
                                onBulkArchiveAnimationFinished = { itemId ->
                                    archivingIds = archivingIds - itemId
                                    viewModel.updateDownloadsArchived(
                                        downloadIds = setOf(itemId),
                                        archived = !isArchived,
                                    )
                                },
                                onToggleSeen = { itemId ->
                                    viewModel.toggleDownloadsSeen(setOf(itemId))
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
    archivingIds: Set<Long>,
    pendingDeleteIds: Set<Long>,
    archived: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    hasAvailableUpdateBanner: Boolean,
    searchQuery: String,
    lazyListState: LazyListState,
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
    val itemIds = remember(items) {
        items.map(DownloadItemUiData::id)
    }
    val animatingOutIds = remember { mutableStateMapOf<Long, Boolean>() }
    val swipeActionIds = remember { mutableStateMapOf<Long, DownloadSwipeActionUiData>() }
    var trackedAutoScrollDownloadId by remember { mutableStateOf<Long?>(null) }
    var trackedAutoScrollWasActive by remember { mutableStateOf(false) }

    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val atBottom = lastVisibleIndex == layoutInfo.totalItemsCount - 1
            !atBottom && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
        }
    }
    var previousItemIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var previousPendingDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
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
            trackedAutoScrollDownloadId =
                itemIds.firstOrNull { it !in previousItemIds && it !in previousPendingDeleteIds }
            trackedAutoScrollWasActive = items
                .firstOrNull { it.id == trackedAutoScrollDownloadId }
                ?.status
                ?.isActive == true
            lazyListState.scrollToItem(0)
        }
        previousItemIds = itemIds
        previousPendingDeleteIds = pendingDeleteIds
    }
    LaunchedEffect(restoringItemIds, itemIds) {
        if (shouldScrollToTopOnRestore(
                restoredItemIds = restoringItemIds,
                currentItemIds = itemIds,
                firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset,
            )
        ) {
            lazyListState.scrollToItem(0)
        }
    }
    LaunchedEffect(items.map { it.id to it.status }, trackedAutoScrollDownloadId) {
        val trackedId = trackedAutoScrollDownloadId ?: return@LaunchedEffect
        val trackedIndex = items.indexOfFirst { it.id == trackedId }
        if (trackedIndex < 0) {
            trackedAutoScrollDownloadId = null
            trackedAutoScrollWasActive = false
            return@LaunchedEffect
        }
        val currentStatus = items[trackedIndex].status
        val isActive = currentStatus.isActive
        if (trackedAutoScrollWasActive && !isActive) {
            lazyListState.animateScrollToItem(trackedIndex)
            trackedAutoScrollDownloadId = null
            trackedAutoScrollWasActive = false
            return@LaunchedEffect
        }
        trackedAutoScrollWasActive = isActive
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = if (hasAvailableUpdateBanner) dimens.spacingXSmall else dimens.zero,
                bottom = dimens.spacingXSmall,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXXSmall),
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
                    isPendingDismiss = animatingOutIds[item.id] == true
                            || item.id in dismissingIds
                            || item.id in archivingIds
                            || item.id in swipeActionIds,
                    archived = archived,
                    seen = item.seen,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                    onSwipeActionPerformed = { itemId, action ->
                        if (action == DownloadSwipeActionUiData.SEEN) {
                            onToggleSeen(itemId)
                        } else {
                            animatingOutIds[itemId] = true
                            swipeActionIds[itemId] = action
                            onSelectionChange(selectedIds - itemId)
                        }
                    },
                    onDismissAnimationFinished = { itemId ->
                        val swipeAction = swipeActionIds.remove(itemId)
                        if (animatingOutIds.remove(itemId) == true && swipeAction == DownloadSwipeActionUiData.DELETE) {
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
    archived: Boolean,
    seen: Boolean,
    leftSwipeAction: DownloadSwipeActionUiData,
    rightSwipeAction: DownloadSwipeActionUiData,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onSelectionChange: (Set<Long>) -> Unit,
    onSwipeActionPerformed: (Long, DownloadSwipeActionUiData) -> Unit,
    onDismissAnimationFinished: (Long) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val swipeEnabled = selectedIds.isEmpty()
            && (leftSwipeAction != DownloadSwipeActionUiData.NONE || rightSwipeAction != DownloadSwipeActionUiData.NONE)

    val longClickEnabled = selectedIds.isEmpty()
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
    val dismissState = key(item.id, seen, leftSwipeAction, rightSwipeAction) {
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                val swipeAction = getSwipeActionForDismissValue(
                    dismissValue = dismissValue,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                )
                if (!isSnapBackSwipeAction(swipeAction)) {
                    return@rememberSwipeToDismissBoxState true
                }
                if (!hasHandledCurrentSwipe) {
                    hasHandledCurrentSwipe = true
                    pendingSnapBackSwipeAction = swipeAction
                }
                false
            },
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
        visibleState = visibilityState,
        enter = Transitions.listItemEnter,
        exit = Transitions.listItemExit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    rowWidthPx = coordinates.size.width.toFloat()
                },
        ) {
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = swipeEnabled && rightSwipeAction != DownloadSwipeActionUiData.NONE,
                enableDismissFromEndToStart = swipeEnabled && leftSwipeAction != DownloadSwipeActionUiData.NONE,
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
                    hasHandledCurrentSwipe = true
                    if (isSnapBackSwipeAction(swipeAction)) {
                        pendingSnapBackSwipeAction = swipeAction
                        coroutineScope.launch {
                            runCatching {
                                dismissState.reset()
                            }
                        }
                    } else {
                        onSwipeActionPerformed(item.id, swipeAction)
                    }
                },
                backgroundContent = {
                    SwipeActionBackground(
                        archived = archived,
                        seen = seen,
                        leftSwipeAction = leftSwipeAction,
                        rightSwipeAction = rightSwipeAction,
                        offsetPx = runCatching { dismissState.requireOffset() }.getOrDefault(0f),
                    )
                },
            ) {
                DownloadItem(
                    item = item,
                    isSelected = isSelected,
                    longClickEnabled = longClickEnabled,
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
            MaterialTheme.colorScheme.background
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
                val showInlineProgressBar = when (item.status) {
                    DownloadStatusUiData.QUEUED,
                    DownloadStatusUiData.METADATA,
                    DownloadStatusUiData.DOWNLOADING -> true

                    else -> false
                }
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

internal fun shouldScrollToTopOnRestore(
    restoredItemIds: Set<Long>,
    currentItemIds: List<Long>,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): Boolean {
    if (restoredItemIds.isEmpty() || currentItemIds.isEmpty()) {
        return false
    }
    val userWasNearTop = firstVisibleItemIndex <= 1 || firstVisibleItemScrollOffset == 0
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
