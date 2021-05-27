package de.dpd.vanassist.util.json

import android.util.Log
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList
import com.mapbox.geojson.Point
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.util.VehicleState

class VehicleJSONParser: JSONParser() {
    companion object {
        fun createHeaderGetCurrentPosition(uid: String): JSONObject {
            val header = JSONObject()
            header.put("uid", uid)

            return header
        }

        fun createHeaderSetProblemSolved(uid: String): JSONObject {
            val header = JSONObject()
            header.put("uid", uid)

            return header
        }

        fun createHeaderSendTestProblem(uid: String): JSONObject {
            val header = JSONObject()
            header.put("uid", uid)

            return header
        }

        fun createHeaderSendDoorStatus(uid: String): JSONObject {
            val header = JSONObject()
            header.put("uid", uid)

            return header
        }

        fun createBodySendTestProblem(message: String): JSONObject {
            val body = JSONObject()
            body.put("problem_message", message)

            return body
        }

        fun createBodySendDoorStatus(doorStatus: String): JSONObject {
            val body = JSONObject()
            body.put("door_status", doorStatus)

            return body
        }

        fun createRequestEmptyBody():JSONObject {
            return JSONObject()
        }

        fun parseResponseToLocation(response: JSONObject): Point {

            val data = response.getJSONObject("data")
            Log.i("VehicleJSONParser", data.toString())

            val position = data.getJSONObject("position")
            Log.i("VehicleJSONParser", position.toString())
            val lat = position.getDouble("lat")
            val lon = position.getDouble("lon")
            Log.i("VehicleJSONParser", "lat: " + lat + " lon: " + lon)

            return Point.fromLngLat(lon, lat)
        }

        fun parseResponseToState(response: JSONObject): VehicleState {
            val data = response.getJSONObject("data")

            val latitude = data.getDouble("latitude")
            val longitude = data.getDouble("longitude")
            val isParking = data.getBoolean("is_parking")
            val doorStatus = data.getString("door_status")
            val logisticStatus = data.getString("logistic_status")
            val problemStatus = data.getString("problem_status")
            val problemMessage = data.getString("problem_message")

            return VehicleState(latitude, longitude, isParking, doorStatus, logisticStatus, problemStatus, problemMessage)
        }

        fun parseVehicleStatusFromShort(status: Short): String {
            when (status) {
                0.toShort() -> return "IDLE"
                1.toShort() -> return "AUTO_DRIVING"
                2.toShort() -> return "MANUAL_DRIVING"
                3.toShort() -> return "PARKING"
                4.toShort() -> return "WAITING"
                5.toShort() -> return "DOORSOPEN"
                6.toShort() -> return "INTERVENTION"
                7.toShort() -> return "HARDFAULT"
                else -> return ""
            }
        }

        fun parseVehicleDoorStatusFromVehicleStatus(status: Short): String {
            when (status) {
                0.toShort() -> return "CLOSED"
                1.toShort() -> return "CLOSED"
                2.toShort() -> return "CLOSED"
                3.toShort() -> return "CLOSED"
                4.toShort() -> return "CLOSED"
                5.toShort() -> return "OPEN"
                6.toShort() -> return "CLOSED"
                7.toShort() -> return "CLOSED"
                else -> return "CLOSED"
            }
        }

        fun parseLogisticStatusFromShort(status: Short): String {
            when (status) {
                0.toShort() -> return "Initialize"
                1.toShort() -> return "Inspection"
                2.toShort() -> return "Inspection Failure"
                10.toShort() -> return "Available"
                11.toShort() -> return "Going to Packing Station"
                12.toShort() -> return "Getting Packed"
                20.toShort() -> return "Ready"
                21.toShort() -> return "Go To Meeting Point"
                30.toShort() -> return "Wait For Courier"
                31.toShort() -> return "In Delivery"
                40.toShort() -> return "Back to Depot"
                41.toShort() -> return "Go to Unpacking Station"
                42.toShort() -> return "Unpacking"
                43.toShort() -> return "Unpacked"
                else -> return ""
            }
        }
    }
}