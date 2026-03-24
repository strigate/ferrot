package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataAsFlowUseCase
import javax.inject.Inject

class DownloadWithMetadataUseCase @Inject constructor(
    val getDownloadsWithMetadataAsFlowUseCase: GetDownloadsWithMetadataAsFlowUseCase,
)
