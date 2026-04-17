package org.strigate.ferrot.domain.model

data class Download(
    val id: Long = 0L,
    val uid: String,
    val url: String,
    val status: DownloadStatus,
    val seen: Boolean,
    val pendingDelete: Boolean = false,
    val archived: Boolean = false,
    val errorMessage: String? = null,
    val completedAtMillis: Long? = null,
)
