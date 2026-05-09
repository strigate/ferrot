package org.strigate.ferrot.domain.model

enum class DownloadSwipeAction(
    val storageValue: String,
) {
    NONE("none"),
    ARCHIVE("archive"),
    SEEN("seen"),
    DELETE("delete");

    companion object {
        fun fromStorageValue(
            value: String?,
            defaultAction: DownloadSwipeAction = DEFAULT,
        ): DownloadSwipeAction {
            return entries.firstOrNull { it.storageValue == value } ?: defaultAction
        }

        val DEFAULT = ARCHIVE
    }
}
