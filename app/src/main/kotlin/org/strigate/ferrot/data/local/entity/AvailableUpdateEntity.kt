package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "available_update")
data class AvailableUpdateEntity(
    @PrimaryKey val id: Int = 0,
    val tag: String,
    val localFilePath: String?,
)
