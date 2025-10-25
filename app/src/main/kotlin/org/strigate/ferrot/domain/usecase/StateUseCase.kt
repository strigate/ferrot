package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.state.GetBootTimeMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveBootTimeMillisUseCase
import javax.inject.Inject

class StateUseCase @Inject constructor(
    val saveBootTimeMillisUseCase: SaveBootTimeMillisUseCase,
    val getBootTimeMillisUseCase: GetBootTimeMillisUseCase,
)
