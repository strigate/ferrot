package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadProgressEntity

@Dao
interface DownloadProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(downloadProgressEntity: DownloadProgressEntity): Long

    @Query("SELECT * FROM download_progress WHERE downloadId = :downloadId")
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadProgressEntity?>

    @Query("UPDATE download_progress SET expectedBytes = :expectedBytes, updatedAtMillis = :updatedAtMillis WHERE downloadId = :downloadId")
    suspend fun updateExpectedBytes(
        downloadId: Long,
        expectedBytes: Long?,
        updatedAtMillis: Long = System.currentTimeMillis(),
    ): Int

    @Query("DELETE FROM download_progress WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
