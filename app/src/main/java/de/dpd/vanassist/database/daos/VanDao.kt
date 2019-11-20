package de.dpd.vanassist.database.daos

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
    fun getVan(id: String): VanEntity
}