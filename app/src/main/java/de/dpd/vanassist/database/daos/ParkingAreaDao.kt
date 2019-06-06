package de.dpd.vanassist.database.daos

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

import de.dpd.vanassist.database.entity.ParkingArea


@Dao
interface ParkingAreaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParkingArea(parkingArea: ParkingArea)

    @Query("SELECT * FROM ParkingArea WHERE id == :id")
    fun getParkingAreaInformation(id: String): ParkingArea

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg parkingArea: ParkingArea)

    @Query("DELETE FROM ParkingArea")
    fun deleteAllFromTable()

    @Query("SELECT * FROM ParkingArea")
    fun getAll(): List<ParkingArea>

}