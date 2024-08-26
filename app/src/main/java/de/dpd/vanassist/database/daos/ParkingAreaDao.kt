package de.dpd.vanassist.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import de.dpd.vanassist.database.entity.ParkingAreaEntity


/* Data Access Object for ParkingAreaEntity */
@Dao
interface ParkingAreaDao {

    /* insert new parkingArea -> Replace ParkingAreaEntity if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParkingArea(parkingArea: ParkingAreaEntity)

    /* Get ParkingAreaEntity by id */
    @Query("SELECT * FROM ParkingArea WHERE id == :id")
    fun getParkingAreaInformation(id: String): ParkingAreaEntity

    /* Inser list of parkingAreas -> Replace ParkingAreaEntity if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg parkingArea: ParkingAreaEntity)

    /* Delete all records from table */
    @Query("DELETE FROM ParkingArea")
    fun deleteAllFromTable()

    /* Get all parkingAreas */
    @Query("SELECT * FROM ParkingArea")
    fun getAll(): List<ParkingAreaEntity>

    @Query( "SELECT * FROM ParkingArea WHERE name == :name")
    fun getParkingAreaByName(name: String): ParkingAreaEntity

}