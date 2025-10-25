package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadmetadata.DeleteDownloadMetadataByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetDownloadMetadataByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.SaveDownloadMetadataUseCase
import javax.inject.Inject

class DownloadMetadataUseCase @Inject constructor(
    val saveDownloadMetadataUseCase: SaveDownloadMetadataUseCase,
    val getDownloadMetadataByIdAsFlowUseCase: GetDownloadMetadataByIdAsFlowUseCase,
    val deleteDownloadMetadataByDownloadIdUseCase: DeleteDownloadMetadataByDownloadIdUseCase,
)
