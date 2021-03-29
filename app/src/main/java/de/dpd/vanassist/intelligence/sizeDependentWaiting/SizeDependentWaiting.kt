package de.dpd.vanassist.intelligence.sizeDependentWaiting

import de.dpd.vanassist.config.ParcelConfig
import de.dpd.vanassist.config.ParkingAreaConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import de.dpd.vanassist.intelligence.intelligentDriving.IntelligentDriving
import de.dpd.vanassist.util.parcel.ParcelUtil
import de.dpd.vanassist.util.parkingArea.ParkingAreaUtil

class SizeDependentWaiting {

    companion object {


        val isEnabled: Boolean
            get() {
                val courier = CourierRepository.shared.getCourier()!!
                if(courier.ambientIntelligenceMode && courier.intelligentDrivingMode && courier.sizeDependentWaitingMode) {
                    return true
                }
                return false
            }


        fun run(fragment:MapFragmentOld) {
            if(isEnabled) {
                val parcel = ParcelRepository.shared.getCurrentParcel()
                if(parcel != null) {
                    if(ParcelUtil.getParcelSize(parcel) == ParcelConfig.XL) {
                        val nextDeliveryLocation = ParcelRepository.shared.getNextParcel()
                        fragment.nextParkingArea = ParkingAreaUtil.getNearestParkingArea(nextDeliveryLocation)!!
                        if (fragment.nextParkingArea == null) {
                            fragment.nextParkingArea =
                                ParkingAreaRepository.shared.getParkingAreaById(ParkingAreaConfig.DEFAULT_PARKING_AREA)
                        }
                        fragment.postNextParkingAreaToServer(fragment.nextParkingArea!!)
                        IntelligentDriving.reset()
                    }
                }
            }
        }
    }
}