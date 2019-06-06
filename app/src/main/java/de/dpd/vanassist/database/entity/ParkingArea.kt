package de.dpd.vanassist.database.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.ColumnInfo

@Entity(tableName = "ParkingArea")
data class ParkingArea(
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
