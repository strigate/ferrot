package org.strigate.ferrot.presentation.model

data class UpdatesUiData(
    val automaticUpdates: Boolean,
    val automaticDependencyUpdates: Boolean,
    val lastAvailableUpdateCheckMillis: Long,
    val lastDependencyUpdateCheckMillis: Long,
)
