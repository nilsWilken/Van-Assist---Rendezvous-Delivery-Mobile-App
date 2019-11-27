package de.dpd.vanassist.database.repository

import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.CourierEntity

class CourierRepository {
    private val courierDao = AppDatabase.shared.courierDao()

    companion object {
        /* Access variable for CourierRepository */
        val shared: CourierRepository by lazy {
            CourierRepository()
        }
    }

    /* Loads the ID of the courier from the database */
    fun getCourierId(): String? = getCourier()?.id

    /* Loads Courier from database */
    fun getCourier(): CourierEntity? = if (getAll().count() == 1) getAll().first() else null

    /* Get all records from the courier table */
    fun getAll(): List<CourierEntity> {
        return courierDao.getAll()
    }

    /* Insert courier record */
    fun insert(courier: CourierEntity) {
        courierDao.insertCourier(courier)
    }

    /* updates the verification token */
    fun updateVerificationToken(verificationToken: String) {
        getCourierId()?.let { courierId ->
            courierDao.updateCourierVerificationToken(courierId, verificationToken)
        }
    }

    /* updated the language code */
    fun updateLanguageCode(languageCode: String) {
        getCourier()?.let { courier ->
            courier.languageCode = languageCode
            courierDao.insertCourier(courier)
        }
    }

    /* delete all records from the courier table */
    fun deleteAll() {
        courierDao.deleteAllFromTable()
    }
}