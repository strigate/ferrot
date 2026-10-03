package org.strigate.ferrot.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity

@Dao
interface AvailableUpdateDao {
    @Query("SELECT * FROM available_update WHERE localFilePath IS NOT NULL LIMIT 1")
    fun get(): Flow<AvailableUpdateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(availableUpdateEntity: AvailableUpdateEntity)

    @Query("DELETE FROM available_update")
    suspend fun delete(): Int
}
