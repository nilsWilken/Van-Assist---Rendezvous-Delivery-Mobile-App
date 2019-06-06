package de.dpd.vanassist.util.parkingArea

import android.content.Context
import de.dpd.vanassist.database.entity.ParkingArea
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.location.LocationUtil
import de.dpd.vanassist.util.toast.Toast

class ParkingAreaUtil {

    companion object {
        fun getNearestParkingArea(context:Context):ParkingArea? {

            val nextDeliveryLocation = ParcelRepository(context).getNextParcelToDeliver()

            val deliveryLat = nextDeliveryLocation!!.latitude
            val deliveryLng = nextDeliveryLocation.longitude
            val parkingAreaList = ParkingAreaRepository(context).getAll()

            var parkingAreaWithShortestDistance:ParkingArea? = null
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