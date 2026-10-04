package org.strigate.ferrot.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "available_update")
data class AvailableUpdateEntity(
    @PrimaryKey val id: Int = 0,
    val tag: String,
    val localFilePath: String?,
)
