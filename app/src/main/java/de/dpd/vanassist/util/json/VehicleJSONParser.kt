package de.dpd.vanassist.util.json

import android.util.Log
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList
import com.mapbox.geojson.Point
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
    }
}