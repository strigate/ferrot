package org.strigate.ferrot.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "download_metadata",
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
        Index(value = ["source", "videoId"]),
    ],
)
data class DownloadMetadataEntity(
    val downloadId: Long,
    val videoId: String? = null,
    val source: String? = null,
    val title: String? = null,
    val thumbnailFilePath: String? = null,
    val durationSeconds: Int? = null,
)
