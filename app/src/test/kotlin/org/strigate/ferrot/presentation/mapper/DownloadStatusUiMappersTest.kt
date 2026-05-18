package org.strigate.ferrot.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.presentation.model.DownloadStatusUiData

class DownloadStatusUiMappersTest {
    @Test
    fun toUiData_mapsEveryDomainStatusToMatchingUiStatus() {
        val mapped = DownloadStatus.entries.associateWith { it.toUiData() }

        assertEquals(DownloadStatusUiData.QUEUED, mapped[DownloadStatus.QUEUED])
        assertEquals(
            DownloadStatusUiData.WAITING_FOR_NETWORK,
            mapped[DownloadStatus.WAITING_FOR_NETWORK]
        )
        assertEquals(DownloadStatusUiData.WAITING_FOR_WIFI, mapped[DownloadStatus.WAITING_FOR_WIFI])
        assertEquals(DownloadStatusUiData.METADATA, mapped[DownloadStatus.METADATA])
        assertEquals(DownloadStatusUiData.DOWNLOADING, mapped[DownloadStatus.DOWNLOADING])
        assertEquals(DownloadStatusUiData.COMPLETED, mapped[DownloadStatus.COMPLETED])
        assertEquals(DownloadStatusUiData.FAILED, mapped[DownloadStatus.FAILED])
        assertEquals(DownloadStatusUiData.STOPPED, mapped[DownloadStatus.STOPPED])
    }
}
