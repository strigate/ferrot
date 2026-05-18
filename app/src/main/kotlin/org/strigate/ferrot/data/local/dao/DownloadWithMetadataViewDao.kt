package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView

@Dao
interface DownloadWithMetadataViewDao {
    @Query(SELECT_DOWNLOADS_QUERY)
    fun getDownloadsAsFlow(): Flow<List<DownloadWithMetadataView>>

    @Query(SELECT_ARCHIVED_DOWNLOADS_QUERY)
    fun getArchivedDownloadsAsFlow(): Flow<List<DownloadWithMetadataView>>

    companion object {
        private const val ORDER_BY = "ORDER BY\n" +
                "  CASE status\n" +
                "    WHEN 'QUEUED' THEN 0\n" +
                "    WHEN 'WAITING_FOR_NETWORK' THEN 1\n" +
                "    WHEN 'WAITING_FOR_WIFI' THEN 2\n" +
                "    WHEN 'METADATA' THEN 3\n" +
                "    WHEN 'DOWNLOADING' THEN 4\n" +
                "    WHEN 'COMPLETED' THEN 5\n" +
                "    WHEN 'FAILED' THEN 6\n" +
                "    WHEN 'STOPPED' THEN 7\n" +
                "    ELSE 8\n" +
                "  END,\n" +
                "  CASE\n" +
                "    WHEN status = 'COMPLETED' THEN completedAtMillis\n" +
                "    WHEN status IN ('DOWNLOADING','METADATA') THEN startedAtMillis\n" +
                "    WHEN status IN ('QUEUED','WAITING_FOR_NETWORK','WAITING_FOR_WIFI','FAILED','STOPPED') THEN enqueuedAtMillis\n" +
                "    ELSE enqueuedAtMillis\n" +
                "  END DESC,\n" +
                "  id DESC"

        private const val SELECT_DOWNLOADS_QUERY =
            "SELECT * FROM downloads_with_metadata_view WHERE archived = 0\n$ORDER_BY"
        private const val SELECT_ARCHIVED_DOWNLOADS_QUERY =
            "SELECT * FROM downloads_with_metadata_view WHERE archived = 1\n$ORDER_BY"
    }
}
