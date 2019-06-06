package de.dpd.vanassist.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import de.dpd.vanassist.database.daos.CourierDao
import de.dpd.vanassist.database.daos.ParcelDao
import de.dpd.vanassist.database.config.DatabaseParameter
import de.dpd.vanassist.database.daos.ParkingAreaDao
import de.dpd.vanassist.database.entity.Courier
import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.database.entity.ParkingArea


@Database(entities = [(Courier::class), (Parcel::class), (ParkingArea::class)], version = DatabaseParameter.VERSION)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courierDao() : CourierDao
    abstract fun parcelDao() : ParcelDao
    abstract fun parkingAreaDao() : ParkingAreaDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getDatabase(context: Context) : AppDatabase {
            if (instance == null) {
                instance = Room
                    .databaseBuilder(context.applicationContext, AppDatabase::class.java, DatabaseParameter.NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }
}