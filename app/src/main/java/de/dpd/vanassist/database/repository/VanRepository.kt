package de.dpd.vanassist.database.repository

import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.daos.VanDao
import de.dpd.vanassist.database.entity.VanEntity

class VanRepository {
    private val vanDao : VanDao = AppDatabase.shared.vanDao()

    companion object {
        val shared: VanRepository by lazy {
            VanRepository()
        }
    }

    /* Insert van record */
    fun insert(van:VanEntity) {
        vanDao.insertVan(van)
    }

    /* Get van by id */
    fun getVanById(vanId:String): VanEntity? {
        return vanDao.getVan(vanId)
    }

    fun updateVanLocationById(id: String, latitude: Double, longitude: Double) {
        vanDao.updateVanLatitude(id, latitude)
        vanDao.updateVanLongitude(id, longitude)
    }

    fun updateVanLogisticStatusById(id: String, logistic_status: String) {
        vanDao.updateVanLogisticStatus(logistic_status, id)
    }

    fun updateVanDoorStatusById(id: String, door_status: String) {
        vanDao.updateVanDoorStatus(door_status, id)
    }

    fun updateVanProblemStatusById(id: String, problem_status: String) {
        vanDao.updateVanProblemStatus(problem_status, id)
    }

    fun updateVanProblemMessageById(id: String, problem_message: String) {
        vanDao.updateVanProblemMessage(problem_message, id)
    }

    fun updateVanById(id: String, latitude: Double, longitude: Double, isParking: Boolean, doorStatus: String,
    logisticStatus: String, problemStatus: String, problemMessage: String) {
        vanDao.updateVanById(id, latitude, longitude, isParking, doorStatus, logisticStatus, problemStatus, problemMessage)
    }
}