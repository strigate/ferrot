package org.strigate.ferrot.presentation.screen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.R
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.helper.PlayHelper
import org.strigate.ferrot.helper.SaveHelper
import org.strigate.ferrot.helper.ShareHelper
import org.strigate.ferrot.presentation.component.ActionIconButton
import org.strigate.ferrot.presentation.component.ConfirmDialog
import org.strigate.ferrot.presentation.component.DownloadProgressSection
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.DownloadEvent
import org.strigate.ferrot.presentation.model.DownloadPageUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.state.DownloadUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.util.UiFormatter
import org.strigate.ferrot.presentation.viewmodel.DownloadViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle(
        initialValue = DownloadMediaType.VIDEO,
    )
    val refreshingMetadataIds by viewModel.refreshingMetadataIds.collectAsStateWithLifecycle()

    val showConfirmDeleteDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DownloadEvent.NavigateBack -> {
                    backDispatcher?.onBackPressed()
                }

                is DownloadEvent.Play -> {
                    PlayHelper.playFileIfExists(context, event.path)
                }

                is DownloadEvent.Share -> {
                    ShareHelper.shareFileIfExists(context, event.path)
                }

                is DownloadEvent.Save -> {
                    SaveHelper.saveToDownloads(context, event.path)
                }
            }
        }
    }

    if (showConfirmDeleteDialog.value) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_delete_download_title),
            message = stringResource(R.string.confirm_dialog_delete_download_description),
            positiveButtonText = stringResource(R.string.yes),
            onPositiveClick = {
                viewModel.deleteDownload()
                showConfirmDeleteDialog.value = false
            },
            negativeButtonText = stringResource(R.string.no),
            onNegativeClick = {
                showConfirmDeleteDialog.value = false
            },
            onDismissRequest = {
                showConfirmDeleteDialog.value = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            backDispatcher?.onBackPressed()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.screen_title_download),
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.markUnseenAndNavigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.content_description_mark_unseen),
                        )
                    }
                    IconButton(
                        onClick = {
                            showConfirmDeleteDialog.value = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.content_description_delete_download),
                        )
                    }
                },
            )
        },
        content = { contentPadding ->
            val dimens = LocalDimens.current
            when (val state = uiState) {
                is DownloadUiState.Loading -> {
                    LoadingState(
                        modifier = modifier
                            .padding(contentPadding)
                            .fillMaxSize(),
                        alignment = Alignment.Center,
                    )
                }

                is DownloadUiState.Data -> {
                    val peekPadding = dimens.spacingMediumAlt
                    val pageSpacing = dimens.spacingSmall
                    DownloadPager(
                        modifier = modifier
                            .padding(contentPadding)
                            .fillMaxSize(),
                        data = state.data,
                        pageDataForId = viewModel::getDownloadPageUiData,
                        selectedId = selectedId,
                        selectedMedia = selectedMedia,
                        refreshingMetadataIds = refreshingMetadataIds,
                        onEnsureDefaults = viewModel::setDefaultsForIds,
                        onDownloadPageSelected = viewModel::selectDownload,
                        onVisibleCompletedUnseenDownload = viewModel::markSeenIfCompleted,
                        onSelectedMedia = { downloadId, type ->
                            viewModel.setSelectedMedia(type, downloadId)
                        },
                        onPlayClick = viewModel::playDownload,
                        onSaveClick = viewModel::saveDownload,
                        onShareClick = viewModel::shareDownload,
                        onRetryClick = viewModel::retryDownload,
                        onRefreshMetadataClick = viewModel::refreshDownloadMetadata,
                        pagePadding = PaddingValues(
                            horizontal = peekPadding,
                            vertical = dimens.zero,
                        ),
                        pageSpacing = pageSpacing,
                    )
                }

                is DownloadUiState.Error -> DownloadError()
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadPager(
    modifier: Modifier = Modifier,
    data: DownloadUiData,
    pageDataForId: (Long) -> Flow<DownloadPageUiData?>,
    selectedId: Long,
    selectedMedia: DownloadMediaType,
    refreshingMetadataIds: Set<Long>,
    onEnsureDefaults: (List<Long>) -> Unit,
    onDownloadPageSelected: (Long) -> Unit,
    onVisibleCompletedUnseenDownload: (Long) -> Unit,
    onSelectedMedia: (Long, DownloadMediaType) -> Unit,
    onPlayClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onRetryClick: (Long) -> Unit,
    onRefreshMetadataClick: (Long) -> Unit,
    pagePadding: PaddingValues,
    pageSpacing: Dp,
) {
    val dimens = LocalDimens.current
    val downloadIds = data.downloadIds
    if (downloadIds.isEmpty()) {
        DownloadError()
    } else {
        val initialIndex = downloadIds
            .indexOfFirst { it == data.id }
            .coerceAtLeast(0)

        val pagerState = rememberPagerState(
            initialPage = initialIndex,
            pageCount = { downloadIds.size },
        )
        val resolvedPage = remember(downloadIds, selectedId) {
            downloadIds.indexOfFirst { it == selectedId }
        }

        LaunchedEffect(downloadIds) {
            onEnsureDefaults(downloadIds)
        }
        LaunchedEffect(resolvedPage) {
            if (resolvedPage >= 0 && pagerState.currentPage != resolvedPage) {
                pagerState.scrollToPage(resolvedPage)
            }
        }
        LaunchedEffect(pagerState, downloadIds) {
            snapshotFlow { pagerState.currentPage }
                .map { pageIndex ->
                    downloadIds.getOrNull(pageIndex)
                }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { downloadId ->
                    onDownloadPageSelected(downloadId)
                }
        }

        HorizontalPager(
            modifier = modifier
                .fillMaxSize(),
            state = pagerState,
            contentPadding = pagePadding,
            pageSpacing = pageSpacing,
            beyondViewportPageCount = 0,
            key = { page -> downloadIds[page] },
        ) { page ->
            val downloadId = downloadIds[page]
            val pageData by remember(downloadId) {
                pageDataForId(downloadId)
            }.collectAsStateWithLifecycle(initialValue = null)
            val isCurrentPage = pagerState.currentPage == page

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dimens.spacingSmall),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = dimens.tonalElevationLow,
                shadowElevation = dimens.shadowElevationLow,
            ) {
                pageData?.let { download ->
                    DownloadPageContent(
                        data = download,
                        isCurrentPage = isCurrentPage,
                        selectedMedia = selectedMedia,
                        isRefreshingMetadata = download.id in refreshingMetadataIds,
                        onCompletedUnseenVisible = onVisibleCompletedUnseenDownload,
                        onMediaChange = { mediaType ->
                            onSelectedMedia(download.id, mediaType)
                        },
                        onEnsureValidSelection = { mediaType ->
                            onSelectedMedia(download.id, mediaType)
                        },
                        onPlayClick = {
                            onPlayClick(download.id)
                        },
                        onSaveClick = {
                            onSaveClick(download.id)
                        },
                        onShareClick = {
                            onShareClick(download.id)
                        },
                        onRetryClick = {
                            onRetryClick(download.id)
                        },
                        onRefreshMetadataClick = {
                            onRefreshMetadataClick(download.id)
                        },
                    )
                } ?: LoadingState(
                    modifier = Modifier
                        .fillMaxSize(),
                    alignment = Alignment.Center,
                )
            }
        }
    }
}

@Composable
private fun DownloadPageContent(
    data: DownloadPageUiData,
    isCurrentPage: Boolean,
    selectedMedia: DownloadMediaType,
    isRefreshingMetadata: Boolean,
    onCompletedUnseenVisible: (Long) -> Unit,
    onMediaChange: (DownloadMediaType) -> Unit,
    onEnsureValidSelection: (DownloadMediaType) -> Unit,
    onPlayClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRefreshMetadataClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    with(data) {
        LaunchedEffect(id, status, seen, isCurrentPage) {
            if (isCurrentPage && status == DownloadStatusUiData.COMPLETED && !seen) {
                onCompletedUnseenVisible(id)
            }
        }
        LaunchedEffect(video?.filePath, audio?.filePath, selectedMedia) {
            val hasVideo = !video?.filePath.isNullOrBlank()
            val hasAudio = !audio?.filePath.isNullOrBlank()
            val fallback = when {
                selectedMedia == DownloadMediaType.VIDEO && !hasVideo && hasAudio -> DownloadMediaType.AUDIO
                selectedMedia == DownloadMediaType.AUDIO && !hasAudio && hasVideo -> DownloadMediaType.VIDEO
                else -> null
            }
            if (fallback != null) {
                onEnsureValidSelection(fallback)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spacingMediumAlt),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))
            Text(
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                text = metadata?.title ?: url,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))
            DownloadProgressSection(
                status = status,
                progressFraction = progress?.progressFraction,
                etaSeconds = progress?.etaSeconds,
                bytesDownloaded = progress?.bytesDownloaded ?: 0L,
                forcePrimaryBar = status == DownloadStatusUiData.COMPLETED,
                completedAtMillis = completedAtMillis,
            )
            Spacer(modifier = Modifier.height(dimens.spacingXSmall))
            MediaSwitcherSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth(),
                selected = selectedMedia,
                enableVideo = video?.filePath?.isNotBlank() == true,
                enableAudio = audio?.filePath?.isNotBlank() == true,
                onSelect = onMediaChange,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))

            val selectedPath = when (selectedMedia) {
                DownloadMediaType.VIDEO -> video?.filePath
                DownloadMediaType.AUDIO -> audio?.filePath
            }
            val canActOnSelected = status == DownloadStatusUiData.COMPLETED
                    && !selectedPath.isNullOrBlank()

            val isMetadataIncomplete = metadata?.thumbnailFilePath.isNullOrBlank()
            val needsMetadataRefresh = status == DownloadStatusUiData.COMPLETED
                    && !status.isActive
                    && isMetadataIncomplete
                    && !isRefreshingMetadata

            ThumbnailCard(
                thumbnailFilePath = metadata?.thumbnailFilePath,
                durationSeconds = metadata?.durationSeconds,
                showRetry = status == DownloadStatusUiData.FAILED || status == DownloadStatusUiData.STOPPED,
                showMetadataRefresh = needsMetadataRefresh,
                showMetadataRefreshLoading = isRefreshingMetadata,
                showPlay = canActOnSelected,
                onPlayClick = onPlayClick,
                onRetryClick = onRetryClick,
                onRefreshMetadataClick = onRefreshMetadataClick,
            )
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val selectedFileExtension = when (selectedMedia) {
                    DownloadMediaType.VIDEO -> video?.fileExtension
                    DownloadMediaType.AUDIO -> audio?.fileExtension
                }?.takeIf {
                    it.isNotBlank()
                }
                selectedFileExtension?.let { fileExtension ->
                    FileExtensionPill(
                        text = fileExtension.uppercase(),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                ActionIconButton(
                    enabled = canActOnSelected,
                    onClick = {
                        if (canActOnSelected) {
                            onSaveClick()
                        }
                    },
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.content_description_save_to_device),
                )
                ActionIconButton(
                    enabled = canActOnSelected,
                    onClick = {
                        if (canActOnSelected) {
                            onShareClick()
                        }
                    },
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.content_description_share_download),
                )
            }
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
            Column {
                MetaItem(
                    label = stringResource(R.string.download_url),
                    isCopyable = true,
                    isUrl = true,
                    value = url,
                )
                val selectedName = when (selectedMedia) {
                    DownloadMediaType.VIDEO -> video?.fileName
                    DownloadMediaType.AUDIO -> audio?.fileName
                }
                selectedName
                    ?.let {
                        MetaItem(
                            label = stringResource(R.string.download_filename),
                            value = it,
                        )
                    }

                metadata?.durationSeconds
                    ?.let(UiFormatter::formatDuration)
                    ?.let { formattedDuration ->
                        MetaItem(
                            label = stringResource(R.string.download_duration),
                            value = formattedDuration,
                        )
                    }

                completedAtMillis
                    ?.takeIf { status == DownloadStatusUiData.COMPLETED }
                    ?.let { completedAt ->
                        MetaItem(
                            label = stringResource(R.string.download_completed_at),
                            value = UiFormatter.formatCompletedAtDetail(
                                context = LocalContext.current,
                                millis = completedAt,
                            ),
                        )
                    }

                errorMessage
                    ?.let {
                        MetaItem(
                            label = stringResource(R.string.download_error_message),
                            value = it,
                        )
                    }
            }
        }
    }
}

@Composable
private fun MediaSwitcherSegmentedButtonRow(
    selected: DownloadMediaType,
    modifier: Modifier = Modifier,
    enableVideo: Boolean,
    enableAudio: Boolean,
    onSelect: (DownloadMediaType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            selected = selected == DownloadMediaType.VIDEO,
            onClick = {
                onSelect(DownloadMediaType.VIDEO)
            },
            enabled = enableVideo,
            label = {
                Text(text = stringResource(R.string.video))
            },
        )
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            selected = selected == DownloadMediaType.AUDIO,
            onClick = {
                onSelect(DownloadMediaType.AUDIO)
            },
            enabled = enableAudio,
            label = {
                Text(text = stringResource(R.string.audio))
            },
        )
    }
}

@Composable
private fun ThumbnailCard(
    thumbnailFilePath: String?,
    durationSeconds: Int?,
    showRetry: Boolean,
    showMetadataRefresh: Boolean,
    showMetadataRefreshLoading: Boolean,
    showPlay: Boolean,
    onPlayClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRefreshMetadataClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    val thumbnailFile = thumbnailFilePath
        ?.let { File(it) }
        ?.takeIf { it.exists() && it.length() > 0 }

    val thumbnailContentDescription = stringResource(R.string.content_description_thumbnail)
    val hasThumbnailFile = thumbnailFile != null
    val showOverlay = showPlay || showRetry
    val onClick = when {
        showRetry -> onRetryClick
        showPlay -> onPlayClick
        else -> null
    }
    val durationText = UiFormatter.formatDuration(durationSeconds)
    val overlayBackgroundColor = Color.Black.copy(alpha = 0.35f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.thumbnailHeight),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = dimens.tonalElevationLow,
    ) {
        val modifier = Modifier.fillMaxSize()
        val clickableModifier = if (onClick != null) {
            modifier
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
        } else {
            modifier
        }
        Box(
            modifier = clickableModifier,
        ) {
            if (hasThumbnailFile) {
                val request = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailFile)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .crossfade(true)
                    .build()

                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    contentDescription = thumbnailContentDescription,
                    model = request,
                )
            } else if (!showRetry) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(dimens.overlayIcon),
                        imageVector = Icons.Filled.Image,
                        contentDescription = thumbnailContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showOverlay) {
                val overlayIcon = if (showRetry) Icons.Filled.Refresh else Icons.Filled.PlayArrow
                val overlayContentDescription = if (showRetry) {
                    stringResource(R.string.content_description_retry)
                } else {
                    stringResource(R.string.content_description_play)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(dimens.overlayButton)
                        .clip(CircleShape)
                        .background(overlayBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tint = Color.White,
                        contentDescription = overlayContentDescription,
                        imageVector = overlayIcon,
                    )
                }
            }
            if (showMetadataRefresh || showMetadataRefreshLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(dimens.spacingSmall)
                        .size(dimens.overlayButtonSmall)
                        .clip(CircleShape)
                        .background(overlayBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showMetadataRefreshLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(dimens.iconXXSmall),
                            color = Color.White,
                            strokeWidth = dimens.spacingXXSmall,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(dimens.overlayButtonSmall)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current,
                                    onClick = onRefreshMetadataClick,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(dimens.iconXXSmall),
                                imageVector = Icons.Filled.Download,
                                contentDescription = stringResource(R.string.content_description_refresh_metadata),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
            if (durationText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimens.spacingSmall)
                        .background(
                            color = overlayBackgroundColor,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(
                            horizontal = dimens.spacingSmall,
                            vertical = dimens.spacingXXSmall,
                        ),
                ) {
                    Text(
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        text = durationText,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileExtensionPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = dimens.shadowElevationLow,
        tonalElevation = dimens.tonalElevationHigh,
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = dimens.spacingSmall,
                    vertical = dimens.spacingXXSmall,
                ),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium,
            text = text,
        )
    }
}

@Composable
private fun MetaItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isCopyable: Boolean = false,
    isUrl: Boolean = false,
) {
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = dimens.spacingXSmall),
        ) {
            Spacer(modifier = Modifier.height(dimens.spacingXSmall))
            Text(
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                text = label,
            )
            Spacer(modifier = Modifier.height(dimens.spacingXXSmall))
            if (isUrl) {
                Text(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                        ) {
                            uriHandler.openUri(value)
                        },
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                    ),
                    text = value,
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
        }
        if (isCopyable) {
            ActionIconButton(
                modifier = Modifier
                    .padding(dimens.zero)
                    .align(Alignment.CenterVertically),
                enabled = true,
                imageVector = Icons.Filled.ContentCopy,
                onClick = {
                    context.copyToClipboard(value, label)
                },
                contentDescription = stringResource(R.string.content_description_copy_to_clipboard),
            )
        }
    }
}

@Composable
private fun DownloadError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier
            .fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_download),
    )
}
