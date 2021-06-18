package de.dpd.vanassist.database.repository

import android.os.Parcel
import de.dpd.vanassist.database.daos.ParcelDao
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.util.parcel.ParcelState

class ParcelRepository {

    companion object {

        private var instance: ParcelRepository? = null
        private var parcelDao : ParcelDao? = null

        /* Access variable for ParcelRepository */
        val shared: ParcelRepository
            get() {
                if (instance == null) {
                    parcelDao = AppDatabase.shared.parcelDao()
                    instance = ParcelRepository()
                }
                return instance!!
            }
    }

    /* Get parcel with lowest delivery position */
    fun getCurrentParcel(): ParcelEntity? {
        val parcelList = getAll()
        var position = parcelList.size +1
        var currentParcel: ParcelEntity? = null
        for(parcel in parcelList) {
            if(parcel.state == ParcelState.PLANNED) {
                if(position == parcelList.size +1 || position > parcel.deliveryPosition) {
                    position = parcel.deliveryPosition
                    currentParcel = parcel
                }
            }
        }
        return currentParcel
    }

    fun getCurrentParcelForParkingArea(parkingArea: String): ParcelEntity? {
        val parcelList = parcelDao!!.getParcelsByParkingAreaName(parkingArea)
        var position = parcelList.size +1
        var currentParcel: ParcelEntity? = null
        for(parcel in parcelList) {
            if(parcel.state == ParcelState.PLANNED) {
                if(position == parcelList.size +1 || position > parcel.deliveryPosition) {
                    position = parcel.deliveryPosition
                    currentParcel = parcel
                }
            }
        }
        return currentParcel
    }

    /* Get parcel with second lowest delivery position*/
    fun getNextParcel(): ParcelEntity? {
        val parcelList = getAll()
        var position = parcelList.size +1
        val currentParcel = getCurrentParcel()
        if(currentParcel == null) {
            return null
        }
        var nextParcel: ParcelEntity? = null
        for(parcel in parcelList) {
            if(parcel.state == ParcelState.PLANNED) {
                if(position == parcelList.size +1 || position > parcel.deliveryPosition)
                    if(parcel.id != currentParcel.id) {
                        position = parcel.deliveryPosition
                        nextParcel = parcel
                    }
            }
        }
        return nextParcel
    }

    /* Loads all parcel from parcel table */
    fun getAll(): List<ParcelEntity> {
        return parcelDao!!.getAll()
    }

    /* get parcel with specific state */
    fun getByState(state: Int): List<ParcelEntity> {
        return parcelDao!!.getParcelsByState(state)
    }

    /* Load whole parcel list */
    fun insert(parcel: ParcelEntity) {
        parcelDao!!.insertParcel(parcel)
    }

    /* insert a list of parcel */
    fun insertAll(parcelList:List<ParcelEntity>) {
        for(parcel in parcelList) {
            parcelDao!!.insertParcel(parcel)
        }
    }

    /* delete all parcel records from parcel list */
    fun deleteAll(){
        parcelDao!!.deleteAllFromTable()
    }

    fun getParcelByDeliveryPosition(deliveryPosition: Int): ParcelEntity {
        return parcelDao!!.getParcelByDeliveryPosition(deliveryPosition)
    }

    fun getParcelById(id: String): ParcelEntity {
        return parcelDao!!.getParcelById(id)
    }

    fun getParcelsByParkingAreaName(parkingArea: String): List<ParcelEntity> {
        return parcelDao!!.getParcelsByParkingAreaName(parkingArea)
    }
}