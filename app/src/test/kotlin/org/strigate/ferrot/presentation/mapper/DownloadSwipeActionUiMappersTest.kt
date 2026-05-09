package org.strigate.ferrot.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData

class DownloadSwipeActionUiMappersTest {
    @Test
    fun toUiData_mapsEveryDomainSwipeAction() {
        val mapped = DownloadSwipeAction.entries.associateWith { it.toUiData() }

        assertEquals(DownloadSwipeActionUiData.NONE, mapped[DownloadSwipeAction.NONE])
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, mapped[DownloadSwipeAction.ARCHIVE])
        assertEquals(DownloadSwipeActionUiData.SEEN, mapped[DownloadSwipeAction.SEEN])
        assertEquals(DownloadSwipeActionUiData.DELETE, mapped[DownloadSwipeAction.DELETE])
    }

    @Test
    fun toDomain_mapsEveryUiSwipeAction() {
        val mapped = DownloadSwipeActionUiData.entries.associateWith { it.toDomain() }

        assertEquals(DownloadSwipeAction.NONE, mapped[DownloadSwipeActionUiData.NONE])
        assertEquals(DownloadSwipeAction.ARCHIVE, mapped[DownloadSwipeActionUiData.ARCHIVE])
        assertEquals(DownloadSwipeAction.SEEN, mapped[DownloadSwipeActionUiData.SEEN])
        assertEquals(DownloadSwipeAction.DELETE, mapped[DownloadSwipeActionUiData.DELETE])
    }
}
