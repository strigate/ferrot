package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "download_progress",
    primaryKeys = ["downloadId"],
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
    ],
)
data class DownloadProgressEntity(
    val downloadId: Long,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val progressPercent: Float = 0F,
    val bytesDownloaded: Long = 0L,
    val etaSeconds: Long? = null,
    val expectedBytes: Long? = null,
)
