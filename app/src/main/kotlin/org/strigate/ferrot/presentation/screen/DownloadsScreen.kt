package org.strigate.ferrot.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .padding(4.dp),
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_appbar),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.app_name),
                        )
                    }
                },
                title = {
                    Text(
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 28.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
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
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.screen_title_settings))
                            },
                            onClick = {
                                navController.navigate(Screen.Settings.route)
                                menuExpanded = false
                            },
                        )
                    }
                },
            )
        },
        content = { contentPadding ->
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
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                            ) {
                                availableUpdate?.let {
                                    AvailableUpdateBanner(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .padding(bottom = 8.dp),
                                        tag = it.tag,
                                        localFilePath = it.localFilePath,
                                        onClick = { filePath ->
                                            viewModel.requestInstallAvailableUpdate(filePath)
                                        },
                                    )
                                }
                                if (downloads.isEmpty()) {
                                    DownloadsIntro(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                    )
                                } else {
                                    DownloadsList(
                                        items = downloads,
                                        onItemClick = {
                                            navController.navigate(
                                                Screen.Download.route(it.id),
                                            )
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

                                                DownloadStatusUiData.PAUSED,
                                                DownloadStatusUiData.STOPPED,
                                                DownloadStatusUiData.FAILED,
                                                DownloadStatusUiData.COMPLETED -> {
                                                    viewModel.retryDownload(item.id)
                                                }
                                            }
                                        },
                                        onDelete = { id ->
                                            viewModel.deleteDownload(id)
                                        },
                                        snackbarHostState = snackbarHostState,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    )
}

@Composable
private fun AvailableUpdateBanner(
    tag: String?,
    localFilePath: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!localFilePath.isNullOrBlank()) {
        Surface(
            modifier = modifier
                .clickable {
                    onClick(localFilePath)
                },
            tonalElevation = 3.dp,
            shadowElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.available_update_ready, tag ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsList(
    items: List<DownloadItemUiData>,
    onItemClick: (DownloadItemUiData) -> Unit,
    onPauseResume: (DownloadItemUiData) -> Unit,
    onDelete: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val listState = rememberLazyListState()
    var activeSwipeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var pendingSnackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastSnackId by rememberSaveable { mutableStateOf<Long?>(null) }

    val snackbarDeletedMessage = stringResource(R.string.snackbar_delete_deleted)
    val snackbarUndoActionLabel = stringResource(R.string.snackbar_delete_undo)

    LaunchedEffect(items) {
        if (activeSwipeId != null && items.none { it.id == activeSwipeId }) {
            activeSwipeId = null
        }
        pendingDeleteIds = pendingDeleteIds.filter { id ->
            items.any { it.id == id }
        }.toSet()

        if (pendingSnackId != null && items.none { it.id == pendingSnackId }) {
            pendingSnackId = null
        }
    }
    LaunchedEffect(pendingDeleteIds) {
        val current = activeSwipeId
        if (current != null && pendingDeleteIds.contains(current)) {
            activeSwipeId = null
        }
    }
    LaunchedEffect(pendingSnackId) {
        val snackId = pendingSnackId ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val previousSnackId = lastSnackId
        if (previousSnackId != null && previousSnackId != snackId) {
            onDelete(previousSnackId)
            pendingDeleteIds = pendingDeleteIds - previousSnackId
        }
        lastSnackId = snackId
        val result = snackbarHostState.showSnackbar(
            message = snackbarDeletedMessage,
            actionLabel = snackbarUndoActionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            pendingDeleteIds = pendingDeleteIds - snackId
        } else {
            onDelete(snackId)
        }
        pendingSnackId = null
        lastSnackId = null
    }
    LifecycleEffect {
        fun delete() {
            val snackId = pendingSnackId
            if (snackId != null) {
                snackbarHostState.currentSnackbarData?.dismiss()
                onDelete(snackId)
                pendingSnackId = null
                pendingDeleteIds = pendingDeleteIds - snackId
                lastSnackId = null
            }
        }
        on(Lifecycle.Event.ON_PAUSE) {
            delete()
        }
        on(Lifecycle.Event.ON_STOP) {
            delete()
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
            runCatching { listState.animateScrollToItem(0) }
        }
    }
    val visibleCount = remember(items, pendingDeleteIds) {
        items.count { it.id !in pendingDeleteIds }
    }
    var introVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(visibleCount) {
        if (visibleCount == 0) {
            introVisible = false
            delay(250)
            introVisible = true
        } else {
            introVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            state = listState,
        ) {
            items(
                items = items,
                key = { it.id },
            ) { item ->
                var rowWidthPx by remember { mutableFloatStateOf(0f) }
                val isVisible = !pendingDeleteIds.contains(item.id)

                AnimatedVisibility(
                    visible = isVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(isVisible) {
                        if (isVisible) {
                            runCatching {
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }
                    }
                    LaunchedEffect(dismissState) {
                        snapshotFlow {
                            val offset = runCatching { dismissState.requireOffset() }
                                .getOrDefault(0f)
                            Pair(dismissState.currentValue, offset)
                        }
                            .distinctUntilChanged()
                            .collectLatest { (value, offsetPx) ->
                                when (value) {
                                    SwipeToDismissBoxValue.Settled -> {
                                        if (activeSwipeId == item.id) activeSwipeId = null
                                    }

                                    else -> {
                                        val lockId = activeSwipeId
                                            ?.takeUnless { pendingDeleteIds.contains(it) }

                                        if (lockId == null) {
                                            activeSwipeId = item.id
                                        } else if (lockId != item.id) {
                                            if (abs(offsetPx) > 1f) {
                                                runCatching {
                                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                }
                                            }
                                            return@collectLatest
                                        }
                                    }
                                }
                                val fullyAtEnd = value == SwipeToDismissBoxValue.EndToStart &&
                                        rowWidthPx > 0f && abs(offsetPx) >= (rowWidthPx - 1f)

                                if (fullyAtEnd) {
                                    pendingDeleteIds = pendingDeleteIds + item.id
                                    if (activeSwipeId == item.id) {
                                        activeSwipeId = null
                                    }
                                    pendingSnackId = item.id
                                }
                            }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .onGloballyPositioned { layoutCoordinates ->
                                rowWidthPx = layoutCoordinates.size.width.toFloat()
                            },
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            gesturesEnabled = true,
                            backgroundContent = {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            DownloadItem(
                                item = item,
                                onClick = { onItemClick(item) },
                                onPauseResume = { onPauseResume(item) },
                                onOpen = { onItemClick(item) },
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = introVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center),
        ) {
            DownloadsIntro()
        }
    }
}

@Composable
private fun DownloadItem(
    item: DownloadItemUiData,
    onClick: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            DownloadPrimaryActionButton(
                status = item.status,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    text = item.title,
                )
                Spacer(modifier = Modifier.height(12.dp))
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
