package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadVideoEntity

@Dao
interface DownloadVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(entity: DownloadVideoEntity): Long

    @Query("SELECT * FROM download_video WHERE downloadId = :downloadId LIMIT 1")
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadVideoEntity?>

    @Query("SELECT downloadId FROM download_video WHERE sha256 = :sha256")
    suspend fun getDownloadIdsBySha256(sha256: String): List<Long>

    @Query("DELETE FROM download_video WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
