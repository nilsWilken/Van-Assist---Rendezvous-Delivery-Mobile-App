package de.dpd.vanassist.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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