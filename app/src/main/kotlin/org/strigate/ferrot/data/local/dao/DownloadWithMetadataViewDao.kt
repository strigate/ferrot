package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView

@Dao
interface DownloadWithMetadataViewDao {
    @Query(SELECT_ALL_QUERY)
    fun getAllAsFlow(): Flow<List<DownloadWithMetadataView>>

    companion object {
        private const val ORDER_BY = "ORDER BY\n" +
                "  CASE status\n" +
                "    WHEN 'QUEUED' THEN 0\n" +
                "    WHEN 'WAITING_FOR_NETWORK' THEN 1\n" +
                "    WHEN 'WAITING_FOR_WIFI' THEN 2\n" +
                "    WHEN 'METADATA' THEN 3\n" +
                "    WHEN 'PAUSED' THEN 4\n" +
                "    WHEN 'DOWNLOADING' THEN 5\n" +
                "    WHEN 'COMPLETED' THEN 6\n" +
                "    WHEN 'FAILED' THEN 7\n" +
                "    WHEN 'STOPPED' THEN 8\n" +
                "    ELSE 9\n" +
                "  END,\n" +
                "  CASE\n" +
                "    WHEN status = 'COMPLETED' THEN completedAtMillis\n" +
                "    WHEN status IN ('DOWNLOADING','PAUSED','METADATA') THEN startedAtMillis\n" +
                "    WHEN status IN ('QUEUED','WAITING_FOR_NETWORK','WAITING_FOR_WIFI','FAILED','STOPPED') THEN enqueuedAtMillis\n" +
                "    ELSE enqueuedAtMillis\n" +
                "  END DESC,\n" +
                "  id DESC"

        private const val SELECT_ALL_QUERY =
            "SELECT * FROM downloads_with_metadata_view\n$ORDER_BY"
    }
}
