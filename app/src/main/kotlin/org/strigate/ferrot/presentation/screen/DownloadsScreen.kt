package org.strigate.ferrot.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.DownloadPrimaryActionButton
import org.strigate.ferrot.presentation.component.DownloadProgressSection
import org.strigate.ferrot.presentation.component.state.EmptyState
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.util.LifecycleEffect
import org.strigate.ferrot.presentation.viewmodel.DownloadsViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    var selectedIds by rememberSaveable {
        mutableStateOf(setOf<Long>())
    }
    var pendingBulkDeleteIds by rememberSaveable {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var bulkPendingCommitIds by rememberSaveable {
        mutableStateOf<Set<Long>>(emptySet())
    }
    var bulkUndoIds by rememberSaveable {
        mutableStateOf<Set<Long>>(emptySet())
    }
    val allIds = when (val state = uiState) {
        is DownloadsUiState.Data -> state.data.downloads.map { it.id }.toSet()
        else -> emptySet()
    }
    val hasDownloads = allIds.isNotEmpty()
    val allSelected = selectedIds.isNotEmpty() && selectedIds.size == allIds.size
    val selectionMode = selectedIds.isNotEmpty()

    BackHandler(enabled = selectionMode) {
        selectedIds = emptySet()
    }
    LifecycleEffect {
        on(Lifecycle.Event.ON_STOP) {
            if (bulkPendingCommitIds.isNotEmpty()) {
                viewModel.deleteDownloads(bulkPendingCommitIds)
                bulkPendingCommitIds = emptySet()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.logShown()
    }

    val snackbarBulkDeletedMessage = stringResource(R.string.snackbar_bulk_delete_deleted)
    val snackbarBulkUndoActionLabel = stringResource(R.string.snackbar_bulk_delete_undo)
    LaunchedEffect(pendingBulkDeleteIds) {
        if (pendingBulkDeleteIds.isEmpty()) {
            return@LaunchedEffect
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        val snackbarResult = snackbarHostState.showSnackbar(
            message = "${pendingBulkDeleteIds.size} $snackbarBulkDeletedMessage",
            actionLabel = snackbarBulkUndoActionLabel,
            duration = SnackbarDuration.Short,
            withDismissAction = true,
        )
        if (snackbarResult == SnackbarResult.ActionPerformed) {
            bulkUndoIds = pendingBulkDeleteIds
            bulkPendingCommitIds = emptySet()
        }
        pendingBulkDeleteIds = emptySet()
    }

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
                                contentDescription = null,
                            )
                        }
                        IconButton(
                            onClick = {
                                bulkPendingCommitIds = selectedIds
                                pendingBulkDeleteIds = selectedIds
                                selectedIds = emptySet()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
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
                        Text(
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        coroutineScope.launch {
                                            lazyListState.animateScrollToItem(0)
                                        }
                                    }
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 28.sp,
                                letterSpacing = 0.sp,
                            ),
                            maxLines = 2,
                            text = stringResource(R.string.app_name),
                        )
                    },
                    actions = {
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
                                .padding(end = 8.dp),
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
                is DownloadsUiState.Loading -> LoadingState()
                is DownloadsUiState.Error -> DownloadsError()
                is DownloadsUiState.Data -> {
                    with(state.data) {
                        if (downloads.isEmpty()) {
                            DownloadsIntro(
                                modifier = Modifier
                                    .fillMaxSize(),
                            )
                        } else {
                            DownloadsList(
                                items = downloads,
                                selectedIds = selectedIds,
                                bulkDeleteIds = pendingBulkDeleteIds,
                                bulkUndoIds = bulkUndoIds,
                                onSelectionChange = {
                                    selectedIds = it
                                },
                                onItemClick = {
                                    navController.navigate(Screen.Download.route(it.id))
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
                                onDelete = {
                                    viewModel.deleteDownload(it)
                                },
                                snackbarHostState = snackbarHostState,
                                lazyListState = lazyListState,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsList(
    items: List<DownloadItemUiData>,
    selectedIds: Set<Long>,
    bulkDeleteIds: Set<Long>,
    bulkUndoIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onDelete: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    lazyListState: LazyListState,
) {
    val dimens = LocalDimens.current
    val coroutineScope = rememberCoroutineScope()

    val showScrollToBottom by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val atBottom = lastVisibleIndex == layoutInfo.totalItemsCount - 1
            !atBottom && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)
        }
    }

    var pendingSnackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(bulkDeleteIds) {
        if (bulkDeleteIds.isNotEmpty()) {
            pendingDeleteIds = pendingDeleteIds + bulkDeleteIds
        }
    }
    LaunchedEffect(bulkUndoIds) {
        if (bulkUndoIds.isNotEmpty()) {
            pendingDeleteIds = pendingDeleteIds - bulkUndoIds
        }
    }

    val snackbarDeletedMessage = stringResource(R.string.snackbar_delete_deleted)
    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)
    LaunchedEffect(pendingSnackId) {
        val snackId = pendingSnackId ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val snackbarResult = snackbarHostState.showSnackbar(
            message = snackbarDeletedMessage,
            actionLabel = snackbarUndoActionLabel,
            duration = SnackbarDuration.Short,
            withDismissAction = true,
        )
        if (snackbarResult == SnackbarResult.ActionPerformed) {
            pendingDeleteIds = pendingDeleteIds - snackId
        } else {
            onDelete(snackId)
        }
        pendingSnackId = null
    }
    LifecycleEffect {
        on(Lifecycle.Event.ON_STOP) {
            pendingSnackId?.let {
                snackbarHostState.currentSnackbarData?.dismiss()
                onDelete(it)
                pendingSnackId = null
                pendingDeleteIds = pendingDeleteIds - it
            }
        }
    }
    LaunchedEffect(items.map { it.id to it.status }) {
        val running = items.any {
            when (it.status) {
                DownloadStatusUiData.QUEUED,
                DownloadStatusUiData.WAITING_FOR_WIFI,
                DownloadStatusUiData.METADATA,
                DownloadStatusUiData.DOWNLOADING -> true

                else -> false
            }
        }
        if (running) {
            runCatching {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                val isSelected = selectedIds.contains(item.id)
                val dismissState = rememberSwipeToDismissBoxState()
                var rowWidthPx by remember {
                    mutableFloatStateOf(0f)
                }
                val isVisible = !pendingDeleteIds.contains(item.id)

                AnimatedVisibility(
                    visible = isVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
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
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = selectedIds.isEmpty(),
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
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        onSelectionChange(
                                            if (isSelected) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        )
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
                                    onItemClick(item)
                                },
                            )
                        }
                    }
                }

                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        runCatching {
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
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
                                onSelectionChange(selectedIds - item.id)
                                pendingDeleteIds = pendingDeleteIds + item.id
                                pendingSnackId = item.id
                            }
                        }
                }
            }
        }
        if (showScrollToBottom) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimens.spacingMedium),
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            index = items.lastIndex,
                        )
                    }
                },
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
                            .size(dimens.iconSmall),
                        imageVector = Icons.Default.ArrowDownward,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
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
                    onClick = onClick,
                    onLongClick = onLongClick,
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
        modifier = modifier,
        text = stringResource(R.string.error_failed_to_load_downloads),
    )
}
