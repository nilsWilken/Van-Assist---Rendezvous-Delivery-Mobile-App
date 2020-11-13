package de.dpd.vanassist.config

class Path {
    companion object {

        /* Base Path */
        const val PORT = "8000"
        //const val BASE_PATH = "http://134.155.108.94:" + PORT + "/"
        const val BASE_PATH = "http://134.155.109.10:" + PORT + "/"
        //const val BASE_PATH = "http://127.0.0.1:" + PORT + "/"
        //const val BASE_PATH = "http://10.0.2.2:" + PORT + "/"

        /* Backend paths */
        const val COURIER_INFORMATION = "courier"
        const val PARCEL_ALL = "parcel/all"
        const val PARCEL_CONFIRM_DELIVERY_SUCCESS = "parcel/delivery/success"
        const val PARCEL_CONFIRM_DELIVERY_FAILURE = "parcel/delivery/failure"
        const val PARCEL_DELIVERY_UNDO = "parcel/delivery/undo"
        const val ENABLE_DARK_MODE = "/courier/darkmode/enable"
        const val DISABLE_DARK_MODE = "/courier/darkmode/disable"
        const val ENABLE_MAP_LABEL = "/courier/help_mode/enable"
        const val DISABLE_MAP_LABEL = "/courier/help_mode/disable"
        const val SIMULATION_START = "/courier/day/start"
        const val TERMS_OF_USE = "<a href=\"https://www.vanassist.de/about/\">Impressum</a>"
        const val SIMULATION_STOP = "/courier/day/finish"
        const val PRIVACY = "https://www.vanassist.de/j/privacy"
        const val PARKING_ALL = "/vehicle/parkingArea/all"
        const val PARKING_NEXT = "vehicle/parkingArea/next"
        const val CHANGE_LANGUAGE = "/courier/language"
        const val PARCEL_ORDER = "parcel/order"
        const val CURRENT_VAN_LOCATION = "/api/v1/fleet/vehicle/currpos"
        const val CURRENT_VAN_STATE = "/api/v1/fleet/vehicle/getCurrentState"
        const val PROBLEM_SOVLED = "/api/v1/fleet/vehicle/exampleID/currentProblemSolved"
        const val SEND_PROBLEM = "/api/v1/fleet/vehicle/exampleID/sendProblem"
        const val SEND_DOOR_STATUS = "/api/v1/fleet/vehicle/setDoorStatus"
        const val PARCEL_STATE_RESET = "/parcel/resetByID"
        const val UPDATE_FCM_TOKEN = "/courier/updateFCMToken"

        const val ENABLE_AMBIENT_INTELLIGENCE_MODE = "courier/ambient_intelligence/enable"
        const val DISABLE_AMBIENT_INTELLIGENCE_MODE = "courier/ambient_intelligence/disable"

        const val ENABLE_DYNAMIC_CONTENT_MODE = "courier/ambient_intelligence/dynamic_content_mode/enable"
        const val DISABLE_DYNAMIC_CONTENT_MODE = "courier/ambient_intelligence/dynamic_content_mode/disable"

        const val ENABLE_SIZE_DEPENDENT_WAITING_MODE = "courier/ambient_intelligence/size_dependent_waiting_mode/enable"
        const val DISABLE_SIZE_DEPENDENT_WAITING_MODE = "courier/ambient_intelligence/size_dependent_waiting_mode/disable"

        const val ENABLE_GAMIFICATION_MODE = "courier/ambient_intelligence/gamification_mode/enable"
        const val DISABLE_GAMIFICATION_MODE = "courier/ambient_intelligence/gamification_mode/disable"

        const val ENABLE_INTELLIGENT_DRIVING_MODE = "courier/ambient_intelligence/intelligent_driving_mode/enable"
        const val DISABLE_INTELLIGENT_DRIVING_MODE = "courier/ambient_intelligence/intelligent_driving_mode/disable"

        const val ENABLE_TIME_BASED_DARK_MODE = "courier/ambient_intelligence/time_based_darkmode/enable"
        const val DISABLE_TIME_BASED_DARK_MODE = "courier/ambient_intelligence/time_based_darkmode/disable"
    }
}