package org.strigate.ferrot.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "download",
    indices = [
        Index("pendingDelete"),
        Index("archived"),
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
    val seen: Boolean = false,
    val pendingDelete: Boolean = false,
    val archived: Boolean = false,
    val errorMessage: String? = null,
    val enqueuedAtMillis: Long = System.currentTimeMillis(),
    val startedAtMillis: Long? = null,
    val completedAtMillis: Long? = null,
)
