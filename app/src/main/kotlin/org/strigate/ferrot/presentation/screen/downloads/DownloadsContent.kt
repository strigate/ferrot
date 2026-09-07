package org.strigate.ferrot.presentation.screen.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.component.state.EmptyState
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.transitions.Transitions
import kotlin.time.Duration.Companion.milliseconds

private const val GRID_SWIPE_REMOVAL_DELAY_MILLIS = 357L
private const val LIST_SWIPE_REMOVAL_DELAY_MILLIS = 120L

private val DOWNLOAD_GRID_MIN_CELL_WIDTH = 160.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsContent(
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
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = visibleCount == 0,
            enter = Transitions.emptyEnter,
            exit = Transitions.emptyExit,
        ) {
            if (searchQuery.isNotBlank()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Filled.Search,
                    title = stringResource(
                        R.string.search_no_results_title,
                        searchQuery,
                    ),
                    body = stringResource(R.string.search_no_results_body),
                )
            } else {
                DownloadsIntro(
                    modifier = Modifier.fillMaxSize(),
                    archived = archived,
                )
            }
        }
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = if (gridLayoutEnabled) {
                GridCells.Adaptive(DOWNLOAD_GRID_MIN_CELL_WIDTH)
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
                SwipeableDownloadItem(
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
                            return@SwipeableDownloadItem
                        }
                        val shouldScrollToTop = shouldScrollToTopOnPauseResume(clickedItem.status)
                        onPauseResume(clickedItem)
                        if (shouldScrollToTop) {
                            coroutineScope.launch {
                                lazyGridState.animateScrollToItem(0)
                            }
                        }
                    },
                    onToggleSelection = { itemId ->
                        onSelectionChange(toggleSelection(selectedIds, itemId))
                    },
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
                                    GRID_SWIPE_REMOVAL_DELAY_MILLIS
                                } else {
                                    LIST_SWIPE_REMOVAL_DELAY_MILLIS
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
                modifier = Modifier.align(Alignment.BottomEnd),
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
                modifier = Modifier.size(dimens.iconXSmall),
                imageVector = Icons.Default.ArrowDownward,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = null,
            )
        }
    }
}

private fun shouldScrollToTopOnPauseResume(status: DownloadStatusUiData): Boolean = !status.isActive

private fun toggleSelection(selectedIds: Set<Long>, itemId: Long): Set<Long> {
    return if (itemId in selectedIds) {
        selectedIds - itemId
    } else {
        selectedIds + itemId
    }
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
