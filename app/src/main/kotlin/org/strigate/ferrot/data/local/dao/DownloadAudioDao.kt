package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadAudioEntity

@Dao
interface DownloadAudioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(entity: DownloadAudioEntity): Long

    @Query("SELECT * FROM download_audio WHERE downloadId = :downloadId LIMIT 1")
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadAudioEntity?>

    @Query("DELETE FROM download_audio WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
