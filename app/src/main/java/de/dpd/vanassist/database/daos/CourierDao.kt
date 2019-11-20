package de.dpd.vanassist.database.daos

import androidx.room.*
import de.dpd.vanassist.database.entity.CourierEntity

/* Data Access Object for CourierEntity */
@Dao
interface CourierDao {

    /* Replace CourierEntity if there already exists an record with the same id */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCourier(courier: CourierEntity)

    /* find courier by id */
    @Query("SELECT * FROM Courier WHERE id == :id")
    fun findOne(id: String): CourierEntity

    /* Get list of couriers */
    @Query("SELECT * FROM Courier")
    fun getAll(): List<CourierEntity>

    /* delete all records from corier list */
    @Query("DELETE FROM Courier")
    fun deleteAllFromTable()

    /* Insert a set of courier */
    @Insert
    fun insertAll(vararg courier: CourierEntity)

    /* Update helpMode for specific courier */
    @Query("UPDATE Courier SET help_mode = :helpMode WHERE id = :id")
    fun updateCourier(id: String, helpMode: Boolean)

    /* Update verificationToken for specific courier */
    @Query("UPDATE Courier SET verification_token = :verificationToken WHERE id = :id")
    fun updateCourierVerificationToken(id:String, verificationToken:String)

    /* Update languageCode for specific courier */
    @Query("UPDATE Courier SET language_code = :languageCode WHERE id = :id")
    fun updateCourierLanguageCode(id:String, languageCode:String)
}