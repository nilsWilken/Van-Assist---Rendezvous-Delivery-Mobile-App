package de.dpd.vanassist.config

class Path {
    companion object {

        /* Base Path */
        const val PORT = "8000"
        //const val BASE_PATH = "http://134.155.108.94:" + PORT + "/"
        const val BASE_PATH = "http://134.155.108.132:" + PORT + "/"

        /* Backend paths */
        const val COURIER_INFORMATION = "courier"
        const val PARCEL_ALL = "parcel/all"
        const val PARCEL_CONFIRM_DELIVERY_SUCCESS = "parcel/delivery/success"
        const val PARCEL_CONFIRM_DELIVERY_FAILURE = "parcel/delivery/failure"
        const val PARCEL_DELIVERY_UNDO = "parcel/delivery/undo"
        const val ENABLE_DARK_MODE = "/courier/darkmode/enable"
        const val DISABLE_DARK_MODE = "/courier/darkmode/disable"
        const val ENABLE_MAP_LABEL = "/courier/label/enable"
        const val DISABLE_MAP_LABEL = "/courier/label/disable"
        const val SIMULATION_START = "/courier/day/start"
        const val TERMS_OF_USE = "<a href=\"https://www.vanassist.de/about/\">Impressum</a>"
        const val SIMULATION_RUNNING = "/courier/simulation/running"
        const val SIMULATION_STOP = "/courier/day/finish"
        const val PRIVACY = "https://www.vanassist.de/j/privacy"
        const val PARKING_ALL = "/vehicle/parkingArea/all"
        const val PARKING_LOAD = "/vehicle/parkingArea/load"
        const val PARKING_NEXT = "vehicle/parkingArea/next"
        const val CHANGE_LANGUAGE = "/courier/language"
        const val PARCEL_ORDER = "parcel/order"
    }
}