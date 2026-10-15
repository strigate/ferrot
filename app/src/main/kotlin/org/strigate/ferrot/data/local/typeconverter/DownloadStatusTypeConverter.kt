package org.strigate.ferrot.data.local.typeconverter

import androidx.room3.ColumnTypeConverter
import org.strigate.ferrot.data.local.entity.DownloadStatus

class DownloadStatusTypeConverter {
    @ColumnTypeConverter
    fun fromStatus(value: DownloadStatus): String = value.name

    @ColumnTypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
