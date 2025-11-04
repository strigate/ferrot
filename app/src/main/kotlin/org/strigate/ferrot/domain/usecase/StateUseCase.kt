package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.state.GetBootTimeMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastDependencyUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveBootTimeMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveLastDependencyUpdateCheckMillisUseCase
import javax.inject.Inject

class StateUseCase @Inject constructor(
    val saveBootTimeMillisUseCase: SaveBootTimeMillisUseCase,
    val getBootTimeMillisUseCase: GetBootTimeMillisUseCase,
    val saveLastAvailableUpdateCheckMillisUseCase: SaveLastAvailableUpdateCheckMillisUseCase,
    val getLastAvailableUpdateCheckMillisUseCase: GetLastAvailableUpdateCheckMillisUseCase,
    val saveLastDependencyUpdateCheckMillisUseCase: SaveLastDependencyUpdateCheckMillisUseCase,
    val getLastDependencyUpdateCheckMillisUseCase: GetLastDependencyUpdateCheckMillisUseCase,
)
