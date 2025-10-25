package org.strigate.ferrot.app

import org.strigate.ferrot.BuildConfig

object Constants {
    const val APP_ID = BuildConfig.APPLICATION_ID

    const val NAME = "Ferrot"
    const val NAME_INTERNAL = "ferrot"
    const val LOG_TAG = "_$NAME_INTERNAL"

    object Database {
        const val DATABASE_NAME = "$NAME_INTERNAL.db"
    }

    object Paths {
        const val DOWNLOADS = "downloads"
        const val UPDATES = "updates"
    }

    object Settings {
        const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
        const val DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY = true
    }

    object Notifications {
        object ChannelGroups {
            private const val CHANNEL_GROUP_ID = "$APP_ID.notification.channel.group.id"
            const val CHANNEL_GROUP_ID_FOREGROUND = "$CHANNEL_GROUP_ID.FOREGROUND"
            const val CHANNEL_GROUP_ID_GENERAL = "$CHANNEL_GROUP_ID.GENERAL"

            val ALL_CHANNEL_GROUP_IDS = listOf(
                CHANNEL_GROUP_ID_FOREGROUND,
                CHANNEL_GROUP_ID_GENERAL,
            )
        }

        object Channels {
            private const val CHANNEL_ID = "$APP_ID.notification.channel.id"
            const val CHANNEL_ID_ACTIVE_DOWNLOADS = "$CHANNEL_ID.ACTIVE_DOWNLOADS"
            const val CHANNEL_ID_DOWNLOADED = "$CHANNEL_ID.DOWNLOADED"

            val ALL_CHANNEL_IDS = listOf(
                CHANNEL_ID_ACTIVE_DOWNLOADS,
                CHANNEL_ID_DOWNLOADED,
            )
        }

        object Groups {
            private const val GROUP_ID = "$APP_ID.notification.group.id"
            const val GROUP_ID_DOWNLOADED = "${GROUP_ID}.DOWNLOADED"
        }
    }

    object Work {
        object Name {
            private const val NAME = "$APP_ID.work"
            private const val KEY = "$APP_ID.key"

            private const val ONETIME = "$NAME.onetime"
            const val DOWNLOAD = "$ONETIME.DOWNLOAD"

            private const val PERIODIC = "$NAME.periodic"
            const val DOWNLOAD_AVAILABLE_UPDATE = "$PERIODIC.DOWNLOAD_AVAILABLE_UPDATE"
            const val UPDATE_DEPENDENCIES = "$PERIODIC.UPDATE_DEPENDENCIES"

            const val KEY_ID = "$KEY.ID"
            const val KEY_WIFI_ONLY = "$KEY.wifi_only"
        }
    }

    object State {
        const val KEY_BOOT_TIME_MILLIS = "boot_time_millis"
        const val DEFAULT_VALUE_BOOT_TIME_MILLIS = 0L
    }

    object Extras {
        private const val EXTRA = "$APP_ID.intent.extra"
        const val EXTRA_ACTION = "$EXTRA.ACTION"
        const val EXTRA_SHARED_URL_UID = "$EXTRA.SHARED_URL_UID"
        const val EXTRA_SHARED_URL = "$EXTRA.SHARED_URL"
        const val EXTRA_DOWNLOAD_ID = "$EXTRA.download.DOWNLOAD_ID"
    }

    object Action {
        private const val ACTION = "$APP_ID.intent.action"
        const val ACTION_START_DOWNLOAD_FROM_SHARE = "$ACTION.START_DOWNLOAD_FROM_SHARE"
        const val ACTION_NAVIGATE_DOWNLOAD = "$ACTION.navigate.DOWNLOAD"
    }
}
