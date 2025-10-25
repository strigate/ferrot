package org.strigate.ferrot.analytics

object AnalyticsEvents {
    const val DOWNLOAD_STARTED = "download_started"
    const val DOWNLOAD_COMPLETED = "download_completed"
    const val DOWNLOAD_FAILED = "download_failed"

    object Screens {
        const val DOWNLOADS = "downloads_screen"
        const val DOWNLOAD = "download_screen"
        const val SETTINGS = "settings_screen"
        const val ABOUT = "about_screen"
    }
}
