package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.availableupdate.ClearAvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.GetAvailableUpdateAsFlowUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.SaveAvailableUpdateUseCase
import javax.inject.Inject

class AvailableUpdateUseCase @Inject constructor(
    val saveAvailableUpdateUseCase: SaveAvailableUpdateUseCase,
    val getAvailableUpdateAsFlowUseCase: GetAvailableUpdateAsFlowUseCase,
    val clearAvailableUpdateUseCase: ClearAvailableUpdateUseCase,
)
