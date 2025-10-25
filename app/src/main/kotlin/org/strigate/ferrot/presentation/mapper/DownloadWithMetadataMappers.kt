package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.presentation.model.DownloadItemUiData

fun DownloadWithMetadata.toUiData(): DownloadItemUiData {
    val fraction = when {
        expectedBytes != null && expectedBytes > 0 && bytesDownloaded >= 0 -> {
            (bytesDownloaded.toDouble() / expectedBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        }

        progressPercent >= 0f -> (progressPercent / 100f).coerceIn(0f, 1f)
        else -> null
    }
    return DownloadItemUiData(
        id = id,
        title = title,
        url = url,
        status = status.toUiData(),
        progressFraction = fraction,
        etaSeconds = etaSeconds,
        bytesDownloaded = bytesDownloaded,
        expectedBytes = expectedBytes,
    )
}
