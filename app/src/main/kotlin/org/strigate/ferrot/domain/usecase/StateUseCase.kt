package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.state.GetArchivedDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.GetBootTimeMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastDependencyUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveBootTimeMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.SaveLastDependencyUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.ToggleArchivedDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.ToggleDownloadsGridLayoutEnabledUseCase
import javax.inject.Inject

class StateUseCase @Inject constructor(
    val saveBootTimeMillisUseCase: SaveBootTimeMillisUseCase,
    val getBootTimeMillisUseCase: GetBootTimeMillisUseCase,
    val toggleDownloadsGridLayoutEnabledUseCase: ToggleDownloadsGridLayoutEnabledUseCase,
    val getDownloadsGridLayoutEnabledUseCase: GetDownloadsGridLayoutEnabledUseCase,
    val toggleArchivedDownloadsGridLayoutEnabledUseCase: ToggleArchivedDownloadsGridLayoutEnabledUseCase,
    val getArchivedDownloadsGridLayoutEnabledUseCase: GetArchivedDownloadsGridLayoutEnabledUseCase,
    val saveLastAvailableUpdateCheckMillisUseCase: SaveLastAvailableUpdateCheckMillisUseCase,
    val getLastAvailableUpdateCheckMillisUseCase: GetLastAvailableUpdateCheckMillisUseCase,
    val saveLastDependencyUpdateCheckMillisUseCase: SaveLastDependencyUpdateCheckMillisUseCase,
    val getLastDependencyUpdateCheckMillisUseCase: GetLastDependencyUpdateCheckMillisUseCase,
)
