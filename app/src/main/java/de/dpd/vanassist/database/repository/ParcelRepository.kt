package de.dpd.vanassist.database.repository

import android.content.Context
import android.os.AsyncTask
import de.dpd.vanassist.database.daos.ParcelDao
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.util.parcel.ParcelStatus

class ParcelRepository(context : Context) {

    private val parcelDao : ParcelDao

    init {
        val appDatabase = AppDatabase.getDatabase(context)
        parcelDao = appDatabase.parcelDao()
    }

    // Load whole parcel list
    fun insert(parcel: Parcel) {
        parcelDao.insertParcel(parcel)
    }

    fun insertAll(parcelList:List<Parcel>) {
        for(parcel in parcelList) {
            parcelDao.insertParcel(parcel)
        }
    }

    fun getAll(): List<Parcel> {
        return parcelDao.getAll()
    }

    fun getByState(state: Int): List<Parcel> {
        return parcelDao.getParcelsByState(state)
    }


    private fun find(id: String) : Parcel {
        return findAsyncTask(parcelDao).execute(id).get()
    }

    fun deleteAll(){
        parcelDao.deleteAllFromTable()
    }

    fun getNextParcelToDeliver(): Parcel? {
        val parcelList = getAll()
        var position = -1
        var nextParcel: Parcel? = null
        for(parcel in parcelList) {
            if(parcel.state == ParcelStatus.PLANNED) {
                if(position == -1 || position > parcel.deliveryPosition) {
                    position = parcel.deliveryPosition
                    nextParcel = parcel
                }
            }
        }
        return nextParcel
    }


    fun getParcelById(parcelId:String): Parcel {
        return find(parcelId)
    }








    /*********** DO NOT USE ANYMORE *********/

    @Deprecated("Do not use")
    private class findAsyncTask internal constructor(private val mAsyncTaskDao: ParcelDao) : AsyncTask<String, Void, Parcel>() {
        override fun doInBackground(vararg params: String): Parcel {
            return mAsyncTaskDao.getParcelInformation(params[0])
        }
    }

    @Deprecated("Do not use")
    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: ParcelDao) : AsyncTask<Parcel, Void, Void>() {
        override fun doInBackground(vararg params: Parcel): Void? {
            mAsyncTaskDao.insertParcel(params[0])
            return null
        }
    }
}