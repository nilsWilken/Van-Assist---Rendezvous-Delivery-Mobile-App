package de.dpd.vanassist.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Van")
data class VanEntity(

    @PrimaryKey(autoGenerate = false)
    val id: String,

    @ColumnInfo(name = "latitude")
    var latitude: Double,

    @ColumnInfo(name = "longitude")
    var longitude: Double,

    @ColumnInfo(name = "is_parking")
    var isParking: Boolean?
)

