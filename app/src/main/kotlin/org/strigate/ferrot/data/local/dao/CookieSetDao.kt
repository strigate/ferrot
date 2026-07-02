package org.strigate.ferrot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.entity.CookieSetDomainEntity
import org.strigate.ferrot.data.local.entity.CookieSetEntity
import org.strigate.ferrot.data.local.entity.CookieSetWithDomainsEntity

@Dao
interface CookieSetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCookieSet(cookieSet: CookieSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<CookieSetDomainEntity>)

    @Transaction
    @Query("SELECT * FROM cookie_set ORDER BY updatedAtMillis DESC")
    fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomainsEntity>>

    @Transaction
    @Query("SELECT * FROM cookie_set ORDER BY updatedAtMillis DESC")
    suspend fun getAllWithDomains(): List<CookieSetWithDomainsEntity>

    @Query("SELECT DISTINCT cookieSetId FROM cookie_set_domain WHERE domain IN (:domains)")
    suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long>

    @Transaction
    @Query("SELECT * FROM cookie_set WHERE id = :id LIMIT 1")
    suspend fun getByIdWithDomains(id: Long): CookieSetWithDomainsEntity?

    @Query("UPDATE cookie_set SET cookieFilePath = :cookieFilePath, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateCookieFilePathById(
        id: Long,
        cookieFilePath: String,
        updatedAtMillis: Long,
    ): Int

    @Query("UPDATE cookie_set SET lastUsedAtMillis = :lastUsedAtMillis WHERE id = :id")
    suspend fun updateLastUsedAtById(id: Long, lastUsedAtMillis: Long): Int

    @Query("DELETE FROM cookie_set WHERE id = :id")
    suspend fun deleteCookieSetById(id: Long): Int
}
