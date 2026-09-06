package org.strigate.ferrot.presentation.screen.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.presentation.component.state.LoadingState
import org.strigate.ferrot.presentation.model.DownloadPageUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.theme.LocalDimens

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DownloadPager(
    modifier: Modifier = Modifier,
    data: DownloadUiData,
    pageDataForId: @Composable (Long) -> DownloadPageUiData?,
    selectedId: Long?,
    selectedMedia: DownloadMediaType,
    onEnsureDefaults: (List<Long>) -> Unit,
    onDownloadPageSelected: (Long) -> Unit,
    onVisibleCompletedUnseenDownload: (Long) -> Unit,
    onSelectedMedia: (Long, DownloadMediaType) -> Unit,
    onPlayClick: (Long) -> Unit,
    onSaveClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onRetryClick: (Long) -> Unit,
    onPageVisible: (Long) -> Unit,
    onUrlClick: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
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
        val synchronizedIds = remember { mutableStateOf(downloadIds) }
        val synchronizedSelectedId = remember { mutableStateOf(selectedId) }
        val isReanchoring = rememberUpdatedState(
            downloadIds != synchronizedIds.value || selectedId != synchronizedSelectedId.value,
        )
        val currentDownloadIds = rememberUpdatedState(downloadIds)
        val currentSelectedId = rememberUpdatedState(selectedId)
        val currentOnDownloadPageSelected = rememberUpdatedState(onDownloadPageSelected)

        LaunchedEffect(downloadIds) {
            onEnsureDefaults(downloadIds)
        }
        LaunchedEffect(downloadIds, selectedId) {
            if (resolvedPage >= 0) {
                try {
                    if (pagerState.currentPage != resolvedPage) {
                        pagerState.scrollToPage(resolvedPage)
                    }
                } finally {
                    synchronizedIds.value = downloadIds
                    synchronizedSelectedId.value = selectedId
                }
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow {
                if (isReanchoring.value) {
                    null
                } else {
                    currentDownloadIds.value.getOrNull(pagerState.settledPage)
                }
            }
                .mapNotNull { it }
                .distinctUntilChanged()
                .collect { downloadId ->
                    if (downloadId != currentSelectedId.value) {
                        currentOnDownloadPageSelected.value(downloadId)
                    }
                }
        }

        HorizontalPager(
            modifier = modifier.fillMaxSize(),
            state = pagerState,
            contentPadding = pagePadding,
            pageSpacing = pageSpacing,
            beyondViewportPageCount = 0,
            key = { page -> downloadIds[page] },
        ) { page ->
            val downloadId = downloadIds[page]
            val isCurrentPage = pagerState.currentPage == page
            val pageData = pageDataForId(downloadId)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dimens.spacingSmall),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = dimens.tonalElevationLow,
                shadowElevation = dimens.shadowElevationLow,
            ) {
                pageData?.let { download ->
                    DownloadPageContent(
                        data = download,
                        isCurrentPage = isCurrentPage,
                        selectedMedia = selectedMedia,
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
                        onPageVisible = {
                            onPageVisible(download.id)
                        },
                        onUrlClick = onUrlClick,
                        onCopyText = onCopyText,
                    )
                } ?: LoadingState(
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.Center,
                )
            }
        }
    }
}
