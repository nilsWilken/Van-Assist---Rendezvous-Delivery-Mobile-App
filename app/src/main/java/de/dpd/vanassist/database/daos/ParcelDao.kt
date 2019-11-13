package de.dpd.vanassist.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dpd.vanassist.database.entity.Parcel

@Dao
interface ParcelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParcel(parcel: Parcel)

    @Query("SELECT * FROM Parcel WHERE id == :id")
    fun getParcelInformation(id: String): Parcel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg parcel: Parcel)

    @Query("DELETE FROM Parcel")
    fun deleteAllFromTable()

    @Query("SELECT * FROM Parcel")
    fun getAll(): List<Parcel>

    @Query("SELECT * FROM Parcel WHERE state == :state")
    fun getParcelsByState(state: Int) : List<Parcel>
}