package de.dpd.vanassist.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dpd.vanassist.database.entity.ParcelEntity

/* Data Access Object for ParcelEntity */
@Dao
interface ParcelDao {

    /* Insert single parcel -> Replace ParcelEntity if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParcel(parcel: ParcelEntity)

    /* Get ParcelEntity Information by id */
    @Query("SELECT * FROM Parcel WHERE id == :id")
    fun getParcelInformation(id: String): ParcelEntity

    /* Insert a list of parcel -> Replace ParcelEntity if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg parcel: ParcelEntity)

    /* Delete all parcel records from table */
    @Query("DELETE FROM Parcel")
    fun deleteAllFromTable()

    /* Get all parcel records from table */
    @Query("SELECT * FROM Parcel")
    fun getAll(): List<ParcelEntity>

    /* Get all parcel with certain state */
    @Query("SELECT * FROM Parcel WHERE state == :state")
    fun getParcelsByState(state: Int) : List<ParcelEntity>

    @Query( "SELECT * FROM Parcel WHERE deliveryPosition == :deliveryPosition")
    fun getParcelByDeliveryPosition(deliveryPosition: Int): ParcelEntity

    @Query( "SELECT * FROM Parcel WHERE id == :id")
    fun getParcelById(id: String): ParcelEntity

    @Query( "SELECT * FROM Parcel WHERE parkingArea == :parkingArea")
    fun getParcelsByParkingAreaName(parkingArea: String): List<ParcelEntity>
}