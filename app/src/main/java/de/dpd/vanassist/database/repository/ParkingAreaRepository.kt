package de.dpd.vanassist.database.repository

import android.content.Context
import android.os.AsyncTask
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.daos.ParkingAreaDao
import de.dpd.vanassist.database.entity.ParkingArea

class ParkingAreaRepository(context : Context) {

    private val parkingAreaDao : ParkingAreaDao

    init {
        val appDatabase = AppDatabase.getDatabase(context)
        parkingAreaDao = appDatabase.parkingAreaDao()
    }

    // Load whole parcel list
    fun insert(parkingArea: ParkingArea) {
        parkingAreaDao.insertParkingArea(parkingArea)
    }

    fun insertAll(parkingAreaList:List<ParkingArea>) {
        for(pA in parkingAreaList) {
            parkingAreaDao.insertParkingArea(pA)
        }
    }

    fun getAll(): List<ParkingArea> {
        return  parkingAreaDao.getAll()
    }


    private fun find(id: String) : ParkingArea {
        return findAsyncTask( parkingAreaDao).execute(id).get()
    }

    fun deleteAll(){
        parkingAreaDao.deleteAllFromTable()
    }


    fun getParcelById(paId:String): ParkingArea {
        return find(paId)
    }


    /*********** DO NOT USE ANYMORE *********/

    @Deprecated("Do not use")
    private class findAsyncTask internal constructor(private val mAsyncTaskDao: ParkingAreaDao) : AsyncTask<String, Void, ParkingArea>() {
        override fun doInBackground(vararg params: String): ParkingArea {
            return mAsyncTaskDao.getParkingAreaInformation(params[0])
        }
    }

    @Deprecated("Do not use")
    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: ParkingAreaDao) : AsyncTask<ParkingArea, Void, Void>() {
        override fun doInBackground(vararg params: ParkingArea): Void? {
            mAsyncTaskDao.insertParkingArea(params[0])
            return null
        }
    }
}
