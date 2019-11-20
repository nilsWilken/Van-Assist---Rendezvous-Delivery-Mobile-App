package de.dpd.vanassist.intelligence.dynamicContent

import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.gps.Position

class DynamicContent {

    companion object {

        private var courierIsCloseToParkedVan = false
        private var courierWasCloseToParkedVan = false

        var showDynamicContentMode = false

        fun manageDynamicContent() {
            val currentCourierIsCloseToParkedVan = checkIfCourierIsCloseToParkedVan()
            val isCourierCloseToDeliveryLocation = checkIfCourierIsCloseToNextDeliveryLocation()

            /* Checks if driver was close to parked van in last query but now isn't anymore */
            /* sets bool that courier is now not anymore in range of van (wasCloseToVan) */
            if(courierIsCloseToParkedVan && !currentCourierIsCloseToParkedVan) {
                courierIsCloseToParkedVan = false
                courierWasCloseToParkedVan = true
            }

            /* Checks if courier is close to delivery location and was in range to parked van before */
            if(isCourierCloseToDeliveryLocation && courierWasCloseToParkedVan) {
                showDynamicContentMode = true
                courierWasCloseToParkedVan = false
            }

            /* Checks if courier was not close to van at all at changes if he is close now */
            if(courierIsCloseToParkedVan == false) {
                courierIsCloseToParkedVan = currentCourierIsCloseToParkedVan
            }
        }

        fun reset() {
            /* Needed: After reset the courier can still be in range of van */
            courierIsCloseToParkedVan = checkIfCourierIsCloseToParkedVan()
            courierWasCloseToParkedVan = false
            showDynamicContentMode = false
        }


        val isActivated :Boolean
            get() {
                if (isDynamicContentEnabled() && showDynamicContentMode) {
                    return true
                }
                return false
            }


        private fun isDynamicContentEnabled(): Boolean {
            val courier = CourierRepository.shared.getCourier()!!
            if(courier.ambientIntelligenceMode && courier.dynamicContentMode) {
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


        private fun checkIfCourierIsCloseToVan(): Boolean {
            val currentVanPosition = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)

            val vanLatitude = currentVanPosition.latitude
            val vanLongitude = currentVanPosition.longitude

            val courierLatitude = Position.latitude
            val courierLongitude = Position.longitude

            return Position.isDistanceSmallerThanThreshold(VanAssistConfig.DISTANCE_TO_VAN_IN_METER, vanLatitude, vanLongitude, courierLatitude, courierLongitude)
        }


        private fun checkIfCourierIsCloseToNextDeliveryLocation(): Boolean {
            val currentParcel = ParcelRepository.shared.getCurrentParcel()
            if(currentParcel == null) {
                return false
            }
            val parcelLatitude = currentParcel.latitude.toDouble()
            val parcelLongitude = currentParcel.longitude.toDouble()

            val courierLatitude = Position.latitude
            val courierLongitude = Position.longitude

            return Position.isDistanceSmallerThanThreshold(VanAssistConfig.DISTANCE_TO_DELIVERY_LOCATION_IN_METER, parcelLatitude, parcelLongitude, courierLatitude, courierLongitude)
        }
    }
}