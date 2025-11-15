package org.strigate.ferrot.presentation.screen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.strigate.ferrot.R
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.presentation.component.ActionIconButton
import org.strigate.ferrot.presentation.component.ConfirmDialog
import org.strigate.ferrot.presentation.component.DownloadProgressSection
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
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
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMediaFlow.collectAsStateWithLifecycle(initialValue = DownloadMediaType.VIDEO)
    val showConfirmDeleteDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }

    if (showConfirmDeleteDialog.value) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_delete_download_title),
            message = stringResource(R.string.confirm_dialog_delete_download_description),
            positiveButtonText = stringResource(R.string.yes),
            onPositiveClick = {
                viewModel.deleteDownload()
                backDispatcher?.onBackPressed()
                showConfirmDeleteDialog.value = false
            },
            negativeButtonText = stringResource(R.string.no),
            onNegativeClick = { showConfirmDeleteDialog.value = false },
            onDismissRequest = { showConfirmDeleteDialog.value = false },
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
            Surface(
                modifier = modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
            ) {
                when (val state = uiState) {
                    is DownloadUiState.Loading -> LoadingState()
                    is DownloadUiState.Error -> DownloadError()
                    is DownloadUiState.Data -> {
                        DownloadContent(
                            data = state.data,
                            selectedMedia = selectedMedia,
                            onMediaChange = { viewModel.setSelectedMedia(it) },
                            onEnsureValidSelection = { viewModel.setSelectedMedia(it) },
                            onPlayClick = { viewModel.playDownload() },
                            onSaveClick = { viewModel.saveDownload() },
                            onShareClick = { viewModel.shareDownload() },
                            onRetryClick = { viewModel.retryDownload() },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DownloadContent(
    data: DownloadUiData,
    selectedMedia: DownloadMediaType,
    onMediaChange: (DownloadMediaType) -> Unit,
    onEnsureValidSelection: (DownloadMediaType) -> Unit,
    onPlayClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    with(data) {
        LaunchedEffect(audio?.filePath, selectedMedia) {
            if (selectedMedia == DownloadMediaType.AUDIO && audio?.filePath.isNullOrBlank()) {
                onEnsureValidSelection(DownloadMediaType.VIDEO)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.spacingMediumAlt),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                text = metadata?.title ?: url,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))
            DownloadProgressSection(
                status = status,
                progressFraction = progress?.progressFraction,
                etaSeconds = progress?.etaSeconds,
                bytesDownloaded = progress?.bytesDownloaded ?: 0L,
                forcePrimaryBar = status == DownloadStatusUiData.COMPLETED,
            )
            Spacer(modifier = Modifier.height(dimens.spacingXSmall))
            MediaSwitcherSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth(),
                selected = selectedMedia,
                enableVideo = video != null,
                enableAudio = audio != null,
                onSelect = onMediaChange,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))

            val selectedPath = when (selectedMedia) {
                DownloadMediaType.VIDEO -> video?.filePath
                DownloadMediaType.AUDIO -> audio?.filePath
            }
            val canActOnSelected = status == DownloadStatusUiData.COMPLETED
                    && !selectedPath.isNullOrBlank()

            ThumbnailCard(
                thumbnailFilePath = metadata?.thumbnailFilePath,
                durationSeconds = metadata?.durationSeconds,
                showRetry = status == DownloadStatusUiData.FAILED || status == DownloadStatusUiData.STOPPED,
                showPlay = canActOnSelected,
                onPlayClick = onPlayClick,
                onRetryClick = onRetryClick,
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
                selectedName?.let {
                    MetaItem(
                        label = stringResource(R.string.download_filename),
                        value = it,
                    )
                }
                val formattedDuration = UiFormatter.formatDuration(metadata?.durationSeconds)
                formattedDuration?.let {
                    MetaItem(
                        label = stringResource(R.string.download_duration),
                        value = it,
                    )
                }
                errorMessage?.let {
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
    showPlay: Boolean,
    onPlayClick: () -> Unit,
    onRetryClick: () -> Unit,
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
                            .size(dimens.overlayIconSize),
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
                        .size(dimens.overlayButtonSize)
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
        modifier = modifier,
        text = stringResource(R.string.error_failed_to_load_download),
    )
}
