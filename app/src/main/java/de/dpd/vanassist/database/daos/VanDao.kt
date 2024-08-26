package de.dpd.vanassist.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dpd.vanassist.database.entity.VanEntity

/* Data Access Object for VanEntity */
@Dao
interface VanDao {

    /* insert new van -> Replace van if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVan(van: VanEntity)

    /* Get all vans */
    @Query("SELECT * FROM Van")
    fun getAll(): List<VanEntity>

    /* Delete all records from list */
    @Query("DELETE FROM Van")
    fun deleteAllFromTable()

    /* Get van by id */
    @Query("SELECT * FROM Van WHERE id == :id")
    fun getVan(id: String): VanEntity?

    @Query("SELECT * FROM Van WHERE id == :id")
    fun getVanAsFlow(id: String): LiveData<VanEntity>

    /* Update van latitude by id */
    @Query( "UPDATE Van SET latitude = :latitude WHERE id = :id")
    fun updateVanLatitude(id: String, latitude: Double)

    /* Update van longitude by id */
    @Query( "UPDATE Van SET longitude = :longitude WHERE id = :id")
    fun updateVanLongitude(id: String, longitude: Double)

    @Query( "SELECT problem_message FROM Van WHERE id == :id")
    fun getVanProblemMessage(id: String): String

 //   @Query ( "SELECT problem_status FROM Van WHERE id == :id")
 //   fun getVanProblemStatus(id: String): String

    @Query ( "SELECT vehicle_status FROM Van WHERE id == :id")
    fun getVehicleStatus(id: String): String

    @Query ("SELECT door_status FROM Van WHERE id == :id")
    fun getVanDoorStatus(id: String): String

    @Query ("SELECT logistic_status FROM Van WHERE id == :id")
    fun getVanLogisticStatus(id: String): String

    @Query ("UPDATE Van SET door_status = :door_status WHERE id = :id")
    fun updateVanDoorStatus(door_status: String, id: String)

    //@Query ("UPDATE Van SET problem_status = :problem_status WHERE id = :id")
    //fun updateVanProblemStatus(problem_status: String, id: String)

    @Query ("UPDATE Van SET vehicle_status = :vehicle_status WHERE id = :id")
    fun updateVehicleStatus(vehicle_status: String, id: String)

    @Query ("UPDATE Van SET problem_message = :problem_message WHERE id = :id")
    fun updateVanProblemMessage(problem_message: String, id: String)

    @Query ("UPDATE Van SET logistic_status = :logistic_status WHERE id = :id")
    fun updateVanLogisticStatus(logistic_status: String, id: String)

    @Query ("UPDATE Van SET latitude = :latitude, longitude = :longitude, is_parking = :isParking, door_status = :doorStatus, logistic_status = :logisticStatus, vehicle_status = :vehicleStatus, problem_message = :problemMessage WHERE id = :id")
    fun updateVanById(id: String, latitude: Double, longitude: Double, isParking: Boolean, doorStatus: String, logisticStatus: String,
    vehicleStatus: String, problemMessage: String)
}