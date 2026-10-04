package org.strigate.ferrot.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadAudioEntity

@Dao
interface DownloadAudioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(downloadAudioEntity: DownloadAudioEntity): Long

    @Query("SELECT * FROM download_audio WHERE downloadId = :downloadId LIMIT 1")
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadAudioEntity?>

    @Query("SELECT filePath FROM download_audio")
    suspend fun getAllFilePaths(): List<String>

    @Query("DELETE FROM download_audio WHERE downloadId = :downloadId")
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
