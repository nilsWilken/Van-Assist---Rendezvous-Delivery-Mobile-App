package de.dpd.vanassist.database.daos

import android.arch.persistence.room.*
import de.dpd.vanassist.database.entity.Courier

@Dao
interface CourierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCourier(courier: Courier)

    @Query("SELECT * FROM Courier WHERE id == :id")
    fun findOne(id: String): Courier

    @Query("SELECT * FROM Courier")
    fun getAll(): List<Courier>

    @Query("DELETE FROM Courier")
    fun deleteAllFromTable()

    @Insert
    fun insertAll(vararg courier: Courier)

    @Query("UPDATE Courier SET map_label = :mapLabel WHERE id = :id")
    fun updateCourier(id: String, mapLabel: Boolean)

    @Query("UPDATE Courier SET verification_token = :verificationToken WHERE id = :id")
    fun updateCourierVerificationToken(id:String, verificationToken:String)

    @Query("UPDATE Courier SET language_code = :languageCode WHERE id = :id")
    fun updateCourierLanguageCode(id:String, languageCode:String)



}