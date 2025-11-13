package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_audio",
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
data class DownloadAudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val downloadId: Long,
    val filePath: String,
    val fileExtension: String,
)
