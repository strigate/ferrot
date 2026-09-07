package org.strigate.ferrot.presentation.screen.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.transitions.Transitions
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SNAP_BACK_SWIPE_THRESHOLD_RATIO = 0.3f
private const val DISMISS_SWIPE_THRESHOLD_RATIO = 0.5f
private const val GRID_SWIPE_OFFSET_ANIMATION_MILLIS = 280

private val GRID_SWIPE_VELOCITY_THRESHOLD = 125.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableDownloadItem(
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
    onToggleSelection: (Long) -> Unit,
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
        modifier = modifier.zIndex(if (gridSwipeActive) 1f else 0f),
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
                    onToggleSelection(item.id)
                } else {
                    onItemClick(item)
                }
            }
            val onOpen = {
                if (selectedIds.isNotEmpty()) {
                    onToggleSelection(item.id)
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
                            onToggleSelection(item.id)
                        },
                        onPauseResume = {
                            onPauseResume(item)
                        },
                        onOpen = onOpen,
                    )
                } else {
                    DownloadListItem(
                        item = item,
                        isSelected = isSelected,
                        interactionEnabled = interactionEnabled,
                        longClickEnabled = longClickEnabled,
                        onClick = onClick,
                        onLongClick = {
                            onToggleSelection(item.id)
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
                        if (hasHandledCurrentSwipe) return@SwipeToDismissBox
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
            animationSpec = tween(GRID_SWIPE_OFFSET_ANIMATION_MILLIS),
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
            modifier = Modifier.matchParentSize(),
            archived = archived,
            seen = seen,
            leftSwipeAction = leftSwipeAction,
            rightSwipeAction = rightSwipeAction,
            offsetPx = logicalOffset(offsetPx),
        )
        Box(
            modifier = Modifier.offset { IntOffset(offsetPx.roundToInt(), 0) },
        ) {
            content()
        }
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
            modifier = Modifier.fillMaxSize(),
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
