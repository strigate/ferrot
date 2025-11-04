package org.strigate.ferrot.presentation.model

data class UpdatesUiData(
    val settings: UpdatesSettings,
    val info: UpdatesInfo,
)

data class UpdatesSettings(
    val automaticUpdates: Boolean,
    val automaticDependencyUpdates: Boolean,
)

data class UpdatesInfo(
    val lastAvailableUpdateCheckMillis: Long,
    val lastDependencyUpdateCheckMillis: Long,
)
