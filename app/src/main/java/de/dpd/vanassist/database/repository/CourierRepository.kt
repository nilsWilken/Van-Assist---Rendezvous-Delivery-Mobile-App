package de.dpd.vanassist.database.repository

import de.dpd.vanassist.database.daos.CourierDao
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.CourierEntity

class CourierRepository {

    companion object {

        private var instance: CourierRepository? = null
        private var courierDao : CourierDao? = null

        /* Access variable for CourierRepository */
        val shared: CourierRepository
            get() {
                if (instance == null) {
                    courierDao = AppDatabase.shared.courierDao()
                    instance = CourierRepository()
                }
                return instance!!
            }
    }

    /* Loads the ID of the courier from the database */
    fun getCourierId():String? {
        val courier = getCourier()
        if(courier != null) {
            return courier.id
        }
        return null
    }

    /* Loads Courier from database */
    fun getCourier(): CourierEntity? {
        val courierList = courierDao!!.getAll()
        if (courierList.count() == 1) {
            return courierList[0]
        }
        return null
    }

    /* Get all records from the courier table */
    fun getAll(): List<CourierEntity> {
        return courierDao!!.getAll()
    }

    /* Insert courier record */
    fun insert(courier: CourierEntity) {
        courierDao!!.insertCourier(courier)
    }

    /* updates the verification token */
    fun updateVerificationToken(verificationToken:String) {
        val courierId = getCourierId()!!
        courierDao!!.updateCourierVerificationToken(courierId, verificationToken)
    }

    /* updated the language code */
    fun updateLanguageCode(languageCode:String) {
        val courier = getCourier()
        courier!!.languageCode = languageCode
        courierDao!!.insertCourier(courier)
    }

    /* delete all records from the courier table */
    fun deleteAll(){
        courierDao!!.deleteAllFromTable()
    }
}