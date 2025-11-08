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
    val showConfirmDeleteDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }

    if (showConfirmDeleteDialog.value) {
        ConfirmDialog(
            onPositiveClick = {
                viewModel.deleteDownload()
                backDispatcher?.onBackPressed()
                showConfirmDeleteDialog.value = false
            },
            onNegativeClick = {
                showConfirmDeleteDialog.value = false
            },
            onDismissRequest = {
                showConfirmDeleteDialog.value = false
            },
            title = stringResource(R.string.confirm_dialog_delete_download_title),
            message = stringResource(R.string.confirm_dialog_delete_download_description),
            positiveButtonText = stringResource(R.string.yes),
            negativeButtonText = stringResource(R.string.no),
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
                            contentDescription = stringResource(R.string.content_description_back),
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.screen_title_download),
                    )
                },
                actions = {
                    val canSaveOrShare = (uiState as? DownloadUiState.Data)
                        ?.data
                        ?.status == DownloadStatusUiData.COMPLETED

                    Row {
                        ActionIconButton(
                            onClick = {
                                if (canSaveOrShare) {
                                    viewModel.saveDownload()
                                }
                            },
                            enabled = canSaveOrShare,
                            contentDescription = stringResource(R.string.content_description_save_to_device),
                            imageVector = Icons.Filled.Save,
                        )
                        ActionIconButton(
                            onClick = {
                                if (canSaveOrShare) {
                                    viewModel.shareDownload()
                                }
                            },
                            enabled = canSaveOrShare,
                            contentDescription = stringResource(R.string.content_description_share_download),
                            imageVector = Icons.Filled.Share,
                        )
                        ActionIconButton(
                            onClick = {
                                showConfirmDeleteDialog.value = true
                            },
                            enabled = true,
                            contentDescription = stringResource(R.string.content_description_delete_download),
                            imageVector = Icons.Filled.Delete,
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
                            onPlayClick = {
                                viewModel.playDownload()
                            },
                            onRetryClick = {
                                viewModel.retryDownload()
                            },
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
    onPlayClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val dimens = LocalDimens.current
    with(data) {
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
                text = title,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMediumAlt))
            DownloadProgressSection(
                status = status,
                progressFraction = progressFraction,
                etaSeconds = etaSeconds,
                bytesDownloaded = bytesDownloaded,
                forcePrimaryBar = status == DownloadStatusUiData.COMPLETED,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMedium))
            ThumbnailCard(
                thumbnailFilePath = thumbnailFilePath,
                showPlay = status == DownloadStatusUiData.COMPLETED && !videoFilePath.isNullOrBlank(),
                showRetry = status == DownloadStatusUiData.FAILED || status == DownloadStatusUiData.STOPPED,
                onPlayClick = onPlayClick,
                onRetryClick = onRetryClick,
            )
            Spacer(modifier = Modifier.height(dimens.spacingMedium))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
            Column {
                MetaItem(
                    label = stringResource(R.string.download_url),
                    isCopyable = true,
                    isUrl = true,
                    value = url,
                )
                videoFileName?.let {
                    MetaItem(stringResource(R.string.download_filename), it)
                }
                errorMessage?.let {
                    MetaItem(stringResource(R.string.download_error_message), it)
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCard(
    thumbnailFilePath: String?,
    showPlay: Boolean,
    showRetry: Boolean,
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
            } else {
                if (!showRetry) {
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
            }
            if (showOverlay) {
                val overlayIcon = if (showRetry) {
                    Icons.Filled.Refresh
                } else {
                    Icons.Filled.PlayArrow
                }
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
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tint = Color.White,
                        contentDescription = overlayContentDescription,
                        imageVector = overlayIcon,
                    )
                }
            }
        }
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
                    modifier = Modifier.clickable(
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
