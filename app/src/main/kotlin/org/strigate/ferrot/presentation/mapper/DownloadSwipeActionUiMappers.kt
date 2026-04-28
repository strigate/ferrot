package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData

fun DownloadSwipeAction.toUiData(): DownloadSwipeActionUiData = when (this) {
    DownloadSwipeAction.NONE -> DownloadSwipeActionUiData.NONE
    DownloadSwipeAction.ARCHIVE -> DownloadSwipeActionUiData.ARCHIVE
    DownloadSwipeAction.SEEN -> DownloadSwipeActionUiData.SEEN
    DownloadSwipeAction.DELETE -> DownloadSwipeActionUiData.DELETE
}

fun DownloadSwipeActionUiData.toDomain(): DownloadSwipeAction = when (this) {
    DownloadSwipeActionUiData.NONE -> DownloadSwipeAction.NONE
    DownloadSwipeActionUiData.ARCHIVE -> DownloadSwipeAction.ARCHIVE
    DownloadSwipeActionUiData.SEEN -> DownloadSwipeAction.SEEN
    DownloadSwipeActionUiData.DELETE -> DownloadSwipeAction.DELETE
}
