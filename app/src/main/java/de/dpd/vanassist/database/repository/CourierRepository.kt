package de.dpd.vanassist.database.repository

import android.content.Context
import android.os.AsyncTask
import de.dpd.vanassist.database.daos.CourierDao
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.Courier
import java.util.concurrent.ExecutionException


class CourierRepository(context: Context) {
    private val courierDao : CourierDao

    private var foundCourier : Courier? = null


    init {
        val appDatabase = AppDatabase.getDatabase(context)
        courierDao = appDatabase.courierDao()
    }

    fun insert(courier: Courier) {
        courierDao.insertCourier(courier)
    }

    fun getCourier(): Courier? {
        var courierList = getAll()
        if (courierList.count() == 1) {
            return courierList[0]
        }
        return null
    }

    fun getCourierId():String? {
        val courier = getCourier()
        if(courier != null) {
            return courier.id
        }
        return null
    }

    fun updateCourierSettings(courier: Courier) {
        val courierId = getCourierId()!!
        courierDao.updateCourier(courierId, courier.mapLabel)
    }

    fun updateVerificationToken(verificationToken:String) {
        val courierId = getCourierId()!!
        courierDao.updateCourierVerificationToken(courierId, verificationToken)
    }

    fun updateLanguageCode(languageCode:String) {
        val courier = getCourier()
        courier!!.languageCode = languageCode
        courierDao.insertCourier(courier)
    }

    fun deleteAll(){
        courierDao.deleteAllFromTable()
    }



    /*********** DO NOT USE ANYMORE *********/

    @Deprecated("Do not use")
    private fun asyncFinished(result : Courier) {
        foundCourier = result
    }

    @Deprecated("Do not use")
    @Throws(ExecutionException::class, InterruptedException::class)
    fun getAll(): List<Courier> {
        return getAllAsyncTask(courierDao).execute().get()
    }


    @Deprecated("Do not use")
    fun find(id: String) : Courier? {
        return findAsyncTask(courierDao, this).execute(id).get()
    }

    @Deprecated("Do not use")
    private class getAllAsyncTask internal constructor(private val mAsyncTaskDao: CourierDao) :
        android.os.AsyncTask<Void, Void, List<Courier>>() {
        internal var a: List<Courier>? = null

        override fun doInBackground(vararg voids: Void): List<Courier> {
            return mAsyncTaskDao.getAll()
        }
    }

    @Deprecated("Do not use")
    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: CourierDao) : AsyncTask<Courier, Void, Void>() {
        override fun doInBackground(vararg params: Courier): Void? {
            mAsyncTaskDao.insertCourier(params[0])
            return null
        }
    }

    @Deprecated("Do not use")
    private class findAsyncTask internal constructor(private val mAsyncTaskDao: CourierDao, private val delegate : CourierRepository) : AsyncTask<String, Void, Courier>() {
        override fun doInBackground(vararg params: String): Courier {
            val found = mAsyncTaskDao.findOne(params[0])
            return found;
        }

        override fun onPostExecute(result: Courier?) {
            super.onPostExecute(result)
            if (result != null) {
                delegate.asyncFinished(result)
            }
        }
    }

}