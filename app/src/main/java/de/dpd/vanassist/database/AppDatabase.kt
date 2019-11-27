package de.dpd.vanassist.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.dpd.vanassist.config.DatabaseConfig
import de.dpd.vanassist.database.daos.CourierDao
import de.dpd.vanassist.database.daos.ParcelDao
import de.dpd.vanassist.database.daos.ParkingAreaDao
import de.dpd.vanassist.database.daos.VanDao
import de.dpd.vanassist.database.entity.CourierEntity
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.entity.ParkingAreaEntity
import de.dpd.vanassist.database.entity.VanEntity


/* This class configures and instantiates the local database
 * -> Entity Files + Database Version are defined */
@Database(
    entities = [(CourierEntity::class), (ParcelEntity::class), (ParkingAreaEntity::class), (VanEntity::class)],
    version = DatabaseConfig.VERSION
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courierDao(): CourierDao
    abstract fun parcelDao(): ParcelDao
    abstract fun parkingAreaDao(): ParkingAreaDao
    abstract fun vanDao(): VanDao

    companion object {

        lateinit var shared: AppDatabase
            private set

        fun createInstance(context: Context) {
            shared = Room
                .databaseBuilder(context.applicationContext, AppDatabase::class.java, DatabaseConfig.NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}