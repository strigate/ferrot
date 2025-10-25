package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView

@Dao
interface DownloadWithMetadataViewDao {
    @Query(
        """
        SELECT * FROM DownloadWithMetadataView
        ORDER BY
          CASE status
            WHEN 'QUEUED' THEN 0
            WHEN 'WAITING_FOR_NETWORK' THEN 1
            WHEN 'WAITING_FOR_WIFI' THEN 2
            WHEN 'METADATA' THEN 3
            WHEN 'PAUSED' THEN 4
            WHEN 'DOWNLOADING' THEN 5
            WHEN 'COMPLETED' THEN 6
            WHEN 'FAILED' THEN 7
            WHEN 'STOPPED' THEN 8
            ELSE 9
          END,
          id DESC
        """
    )
    fun getAllAsFlow(): Flow<List<DownloadWithMetadataView>>
}
