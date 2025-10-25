package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.DownloadEntity
import org.strigate.ferrot.data.local.entity.DownloadStatus

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insert(downloadEntity: DownloadEntity): Long

    @Query("SELECT * FROM download ORDER BY enqueuedAtMillis DESC")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM download WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM download WHERE id = :id")
    fun getByIdAsFlow(id: Long): Flow<DownloadEntity?>

    @Query("UPDATE download SET filePath = :fileName WHERE id = :id")
    suspend fun updateFilePathById(id: Long, fileName: String?): Int

    @Query("UPDATE download SET status = :status WHERE id = :id")
    suspend fun updateStatusById(id: Long, status: DownloadStatus): Int

    @Query("UPDATE download SET errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateErrorMessageById(id: Long, errorMessage: String?): Int

    @Query("UPDATE download SET startedAtMillis = :startedAtMillis WHERE id = :id")
    suspend fun updateStartedAtById(id: Long, startedAtMillis: Long?): Int

    @Query("UPDATE download SET completedAtMillis = :completedAtMillis WHERE id = :id")
    suspend fun updateCompletedAtById(id: Long, completedAtMillis: Long?): Int

    @Query("DELETE FROM download WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
