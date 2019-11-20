package de.dpd.vanassist.database.repository

import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.daos.ParkingAreaDao
import de.dpd.vanassist.database.entity.ParkingAreaEntity

class ParkingAreaRepository {

    companion object {

        private var instance: ParkingAreaRepository? = null
        private var parkingAreaDao : ParkingAreaDao? = null

        /* Access variable for ParkingAreaRepository */
        val shared: ParkingAreaRepository
            get() {
                if (instance == null) {
                    parkingAreaDao = AppDatabase.shared.parkingAreaDao()
                    instance = ParkingAreaRepository()
                }
                return instance!!
            }
    }

    /* Get parkingArea by id */
    fun getParkingAreaById(paId:String): ParkingAreaEntity {
        return parkingAreaDao!!.getParkingAreaInformation(paId)
    }

    /* Get all parkingAreas */
    fun getAll(): List<ParkingAreaEntity> {
        return  parkingAreaDao!!.getAll()
    }

    /* Load whole parcel list */
    fun insert(parkingArea: ParkingAreaEntity) {
        parkingAreaDao!!.insertParkingArea(parkingArea)
    }

    /* Insert a list of ParkingAreas */
    fun insertAll(parkingAreaList:List<ParkingAreaEntity>) {
        for(pA in parkingAreaList) {
            parkingAreaDao!!.insertParkingArea(pA)
        }
    }

    /* delete all records from parkingAreas */
    fun deleteAll(){
        parkingAreaDao!!.deleteAllFromTable()
    }
}
