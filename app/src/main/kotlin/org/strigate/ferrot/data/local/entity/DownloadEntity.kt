package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download",
    indices = [
        Index("status"),
        Index("enqueuedAtMillis"),
        Index("uid"),
    ],
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uid: String,
    val url: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val enqueuedAtMillis: Long = System.currentTimeMillis(),
    val startedAtMillis: Long? = null,
    val completedAtMillis: Long? = null,
)
