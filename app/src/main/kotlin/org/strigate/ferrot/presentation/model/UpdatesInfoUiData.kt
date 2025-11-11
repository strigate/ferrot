package org.strigate.ferrot.presentation.model

data class UpdatesInfoUiData(
    val lastAvailableUpdateCheckMillis: Long,
    val lastDependencyUpdateCheckMillis: Long,
)
