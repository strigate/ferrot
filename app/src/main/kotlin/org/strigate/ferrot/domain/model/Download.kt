package org.strigate.ferrot.domain.model

data class Download(
    val id: Long = 0L,
    val uid: String,
    val url: String,
    val filePath: String? = null,
    val status: DownloadStatus,
    val errorMessage: String? = null,
)
