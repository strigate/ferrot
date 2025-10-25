package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
    ],
)
data class DownloadMetadataEntity(
    val downloadId: Long,
    val title: String? = null,
    val thumbnailFilePath: String? = null,
)
