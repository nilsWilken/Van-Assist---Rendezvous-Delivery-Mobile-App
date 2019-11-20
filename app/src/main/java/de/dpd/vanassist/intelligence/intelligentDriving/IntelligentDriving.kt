package de.dpd.vanassist.intelligence.intelligentDriving

import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.gps.Position

class IntelligentDriving {

    companion object{

        private var courierIsCloseToParkedVan = false
        private var courierWasCloseToParkedVan = false

        fun manageIntelligentDrivingMode() {
            val currentCourierIsCloseToParkedVan = checkIfCourierIsCloseToParkedVan()

            /* Checks if driver was close to parked van in last query but now isn't anymore
             * sets bool that courier is now not anymore in range of van (wasCloseToVan) */
            if(courierIsCloseToParkedVan && !currentCourierIsCloseToParkedVan) {
                courierIsCloseToParkedVan = false
                courierWasCloseToParkedVan = true
            }

            /* Checks if courier was not close to van at all at changes if he is close now */
            if(courierIsCloseToParkedVan == false) {
                courierIsCloseToParkedVan = currentCourierIsCloseToParkedVan
            }
        }


        val isActivated:Boolean
            get() {
                if(isIntelligentDrivingEnabled() && courierWasCloseToParkedVan) {
                    return true
                }
                return false
            }


        private fun isIntelligentDrivingEnabled(): Boolean {
            val courier = CourierRepository.shared.getCourier()!!
            if(courier.ambientIntelligenceMode && courier.intelligentDrivingMode) {
                return true
            }
            return false
        }


        private fun checkIfVanIsParked(): Boolean {
            return VanRepository.shared.getVanById(VanAssistConfig.VAN_ID).isParking
        }


        private fun checkIfCourierIsCloseToParkedVan():Boolean {
            if(checkIfVanIsParked()) {
                if(checkIfCourierIsCloseToVan()) {
                    return true
                }
            }
            return false
        }


        fun reset() {
            /* Needed: After reset the courier can still be in range of van */
            courierIsCloseToParkedVan = checkIfCourierIsCloseToParkedVan()
            courierWasCloseToParkedVan = false
        }


        private fun checkIfCourierIsCloseToVan(): Boolean {
            val currentVanPosition = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)
            if(currentVanPosition == null) {
                return false;
            }
            val vanLatitude = currentVanPosition.latitude
            val vanLongitude = currentVanPosition.longitude

            val courierLatitude = Position.latitude
            val courierLongitude = Position.longitude

            return Position.isDistanceSmallerThanThreshold(VanAssistConfig.DISTANCE_TO_VAN_IN_METER, vanLatitude, vanLongitude, courierLatitude, courierLongitude)
        }
    }
}