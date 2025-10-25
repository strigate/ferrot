package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity

@Dao
interface AvailableUpdateDao {
    @Query("SELECT * FROM available_update WHERE localFilePath IS NOT NULL LIMIT 1")
    fun get(): Flow<AvailableUpdateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(row: AvailableUpdateEntity)

    @Query("DELETE FROM available_update")
    suspend fun delete(): Int
}
