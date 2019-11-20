package de.dpd.vanassist.util.parkingArea

import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.entity.ParkingAreaEntity
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.location.LocationUtil

class ParkingAreaUtil {

    companion object {
        fun getNearestParkingArea(nextDeliveryLocation: ParcelEntity?):ParkingAreaEntity? {

            if(nextDeliveryLocation == null) {
                return null
            }
            val deliveryLat = nextDeliveryLocation.latitude
            val deliveryLng = nextDeliveryLocation.longitude
            val parkingAreaList = ParkingAreaRepository.shared.getAll()

            var parkingAreaWithShortestDistance:ParkingAreaEntity? = null
            var shortestDistance = 5000000.00
            var counter = 0

            for(parkingArea in parkingAreaList) {
                val lat = parkingArea.lat
                val lng = parkingArea.long_

                val distance = LocationUtil.calculateDistance(deliveryLat.toDouble(), deliveryLng.toDouble(), lat.toDouble(), lng.toDouble())

                if(counter==0){
                    parkingAreaWithShortestDistance = parkingArea
                    shortestDistance = distance
                } else if(distance < shortestDistance) {
                    shortestDistance = distance
                    parkingAreaWithShortestDistance = parkingArea
                }
                counter++
            }
            return parkingAreaWithShortestDistance
        }
    }
}