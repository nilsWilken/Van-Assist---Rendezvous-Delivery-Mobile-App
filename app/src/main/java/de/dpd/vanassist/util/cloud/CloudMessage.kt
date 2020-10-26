package de.dpd.vanassist.util.cloud

class CloudMessage {

    companion object {
        const val SIMULATION_START = "simulation_start"
        const val SIMULATION_STOP = "simulation_stop"
        const val VEHICLE_IS_IN_NEXT_PARKING_AREA = "vehicle_is_in_next_parking_area"
        const val CURRENT_VAN_LOCATION = "current_van_location"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val VAN_PROBLEM = "van_problem"
        const val LOGISTIC_STATUS = "logistic_status"
        const val DOOR_STATUS = "door_status"
    }
}