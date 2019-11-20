package de.dpd.vanassist.gps

class Position {

    companion object {

        var latitude:Double = 0.0
        var longitude:Double = 0.0


        fun update(latitude: Double, longitude: Double) {
            this.latitude = latitude
            this.longitude = longitude
        }


        fun isDistanceSmallerThanThreshold(threshold: Int ,lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean {
            val distance = getDistanceInMeter(lat1, lng1, lat2, lng2)
            return distance < threshold
        }


        private fun getDistanceInMeter(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            /* earthRadius in meters */
            val earthRadius = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return earthRadius * c
        }
    }

}