package org.strigate.ferrot.presentation.screen.download

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.strigate.ferrot.R
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.helper.PlayHelper
import org.strigate.ferrot.helper.SaveHelper
import org.strigate.ferrot.helper.ShareHelper
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.component.ConfirmDialog
import org.strigate.ferrot.presentation.component.state.ErrorState
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.event.DownloadEvent
import org.strigate.ferrot.presentation.model.DownloadPageUiData
import org.strigate.ferrot.presentation.state.DownloadUiState
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.viewmodel.DownloadViewModel
import org.strigate.refinery.theme.RefineryTopAppBarDefaults

@Composable
fun DownloadScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle(
        initialValue = DownloadMediaType.VIDEO,
    )
    val selectedPageData by viewModel.selectedPageData.collectAsStateWithLifecycle()

    val showConfirmDeleteDialog = remember { mutableStateOf(false) }
    val onNavigateBack = {
        navigateBackToParent(
            navController = navController,
            archived = selectedPageData?.archived == true,
        )
    }

    BackHandler(enabled = !showConfirmDeleteDialog.value) {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DownloadEvent.NavigateBack -> {
                    onNavigateBack()
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

    DownloadScreenContent(
        state = DownloadScreenContentState(
            uiState = uiState,
            selectedId = selectedId,
            selectedMedia = selectedMedia,
            selectedPageData = selectedPageData,
            showConfirmDeleteDialog = showConfirmDeleteDialog.value,
        ),
        pageDataForId = { downloadId ->
            val pageData by remember(downloadId) {
                viewModel.getDownloadPageUiData(downloadId)
            }.collectAsStateWithLifecycle(initialValue = null)
            pageData
        },
        onBackClick = onNavigateBack,
        onMarkUnseen = viewModel::markUnseenAndNavigateBack,
        onUpdateArchived = viewModel::updateArchived,
        onShowDeleteConfirmation = { showConfirmDeleteDialog.value = true },
        onDeleteConfirmed = {
            viewModel.deleteDownload()
            showConfirmDeleteDialog.value = false
        },
        onDeleteDismissed = { showConfirmDeleteDialog.value = false },
        onEnsureDefaults = viewModel::setDefaultsForIds,
        onDownloadPageSelected = viewModel::selectDownload,
        onVisibleCompletedUnseenDownload = viewModel::markSeenIfCompleted,
        onSelectedMedia = { downloadId, mediaType ->
            viewModel.setSelectedMedia(mediaType, downloadId)
        },
        onPlayClick = viewModel::playDownload,
        onSaveClick = viewModel::saveDownload,
        onShareClick = viewModel::shareDownload,
        onRetryClick = { downloadId ->
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            viewModel.retryDownload(downloadId)
        },
        onRefreshMetadataClick = viewModel::refreshDownloadMetadata,
        onUrlClick = uriHandler::openUri,
        onCopyText = context::copyToClipboard,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadScreenContent(
    state: DownloadScreenContentState,
    pageDataForId: @Composable (Long) -> DownloadPageUiData?,
    onBackClick: () -> Unit,
    onMarkUnseen: () -> Unit,
    onUpdateArchived: (Boolean) -> Unit,
    onShowDeleteConfirmation: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onEnsureDefaults: (List<Long>) -> Unit,
    onDownloadPageSelected: (Long) -> Unit,
    onVisibleCompletedUnseenDownload: (Long) -> Unit,
    onSelectedMedia: (Long, DownloadMediaType) -> Unit,
    onPlayClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onRetryClick: (Long) -> Unit,
    onRefreshMetadataClick: (Long) -> Unit,
    onUrlClick: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showConfirmDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_dialog_delete_download_title),
            message = stringResource(R.string.confirm_dialog_delete_download_description),
            positiveButtonText = stringResource(R.string.yes),
            isDestructive = true,
            onPositiveClick = onDeleteConfirmed,
            negativeButtonText = stringResource(R.string.no),
            onNegativeClick = onDeleteDismissed,
            onDismissRequest = onDeleteDismissed,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = RefineryTopAppBarDefaults.colors(),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
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
                        text = if (state.selectedPageData?.archived == true) {
                            stringResource(R.string.screen_title_archived_download)
                        } else {
                            stringResource(R.string.screen_title_download)
                        },
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            onMarkUnseen()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.content_description_mark_unseen),
                        )
                    }
                    IconButton(
                        onClick = {
                            val archived = state.selectedPageData?.archived ?: false
                            onUpdateArchived(!archived)
                        },
                    ) {
                        Icon(
                            imageVector = if (state.selectedPageData?.archived == true) {
                                Icons.Filled.Unarchive
                            } else {
                                Icons.Filled.Archive
                            },
                            contentDescription = if (state.selectedPageData?.archived == true) {
                                stringResource(R.string.content_description_unarchive)
                            } else {
                                stringResource(R.string.content_description_archive)
                            },
                        )
                    }
                    IconButton(
                        onClick = {
                            onShowDeleteConfirmation()
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
            when (val uiState = state.uiState) {
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
                        data = uiState.data,
                        pageDataForId = pageDataForId,
                        selectedId = state.selectedId,
                        selectedMedia = state.selectedMedia,
                        onEnsureDefaults = onEnsureDefaults,
                        onDownloadPageSelected = onDownloadPageSelected,
                        onVisibleCompletedUnseenDownload = onVisibleCompletedUnseenDownload,
                        onSelectedMedia = onSelectedMedia,
                        onPlayClick = onPlayClick,
                        onSaveClick = onSaveClick,
                        onShareClick = onShareClick,
                        onRetryClick = onRetryClick,
                        onRefreshMetadataClick = onRefreshMetadataClick,
                        onUrlClick = onUrlClick,
                        onCopyText = onCopyText,
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

private fun navigateBackToParent(
    navController: NavController,
    archived: Boolean,
) {
    val parentRoute = if (archived) {
        Screen.Archived.route
    } else {
        Screen.Downloads.route
    }
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    if (previousRoute == parentRoute) {
        navController.popBackStack()
        return
    }
    if (navController.popBackStack(parentRoute, false)) {
        return
    }
    navController.navigate(parentRoute) {
        popUpTo(Screen.Downloads.route) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

@Composable
internal fun DownloadError(
    modifier: Modifier = Modifier,
) {
    ErrorState(
        modifier = modifier.fillMaxSize(),
        alignment = Alignment.Center,
        text = stringResource(R.string.error_failed_to_load_download),
    )
}

internal data class DownloadScreenContentState(
    val uiState: DownloadUiState,
    val selectedId: Long?,
    val selectedMedia: DownloadMediaType,
    val selectedPageData: DownloadPageUiData?,
    val showConfirmDeleteDialog: Boolean,
)
