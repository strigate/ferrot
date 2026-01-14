package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadIdsWithMetadataAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataAsFlowUseCase
import javax.inject.Inject

class DownloadWithMetadataUseCase @Inject constructor(
    val getDownloadIdsWithMetadataAsFlowUseCase: GetDownloadIdsWithMetadataAsFlowUseCase,
    val getDownloadsWithMetadataAsFlowUseCase: GetDownloadsWithMetadataAsFlowUseCase,
)
