package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadMetadataEntity

@Dao
interface DownloadMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(downloadMetadataEntity: DownloadMetadataEntity): Long

    @Query("SELECT * FROM download_metadata WHERE downloadId = :downloadId")
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadMetadataEntity?>

    @Query("DELETE FROM download_metadata WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
