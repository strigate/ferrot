package org.strigate.ferrot.data.local.typeconverter

import androidx.room.TypeConverter
import org.strigate.ferrot.data.local.entity.DownloadStatus

class DownloadStatusTypeConverter {
    @TypeConverter
    fun fromStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
