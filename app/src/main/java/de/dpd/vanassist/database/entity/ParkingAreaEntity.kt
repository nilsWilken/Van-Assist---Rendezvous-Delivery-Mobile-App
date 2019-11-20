package de.dpd.vanassist.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "ParkingArea")
data class ParkingAreaEntity(
        @PrimaryKey(autoGenerate = false)
        var id : String,

        @ColumnInfo(name = "name")
        var name : String,

        @ColumnInfo(name = "length")
        var length : Float,

        @ColumnInfo(name = "lane")
        var lane : String,

        @ColumnInfo(name = "edge")
        var edge : String,

        @ColumnInfo(name = "roadSideCapacity")
        var roadSideCapacity : Int,

        @ColumnInfo(name = "startPos")
        var startPos : Float,

        @ColumnInfo(name = "endPos")
        var endPos : Float,

        @ColumnInfo(name = "lat")
        var lat : Float,

        @ColumnInfo(name = "long_")
        var long_  : Float,

        @ColumnInfo(name = "x")
        var x : Float,

        @ColumnInfo(name = "y")
        var y : Float
)
{
        constructor() : this("", "", 0f, "", "",1, 0f, 0f, 0f, 0f, 0f, 0f)
}
