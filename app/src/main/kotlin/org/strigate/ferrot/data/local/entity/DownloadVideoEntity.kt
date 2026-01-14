package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_video",
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["downloadId"], unique = true),
        Index(value = ["sha256"]),
    ],
)
data class DownloadVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val downloadId: Long,
    val filePath: String,
    val fileExtension: String,
    val sha256: String?,
)
