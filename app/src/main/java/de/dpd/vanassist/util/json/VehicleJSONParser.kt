package de.dpd.vanassist.util.json

import android.util.Log
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList
import com.mapbox.geojson.Point

class VehicleJSONParser: JSONParser() {
    companion object {
        fun createHeaderGetCurrentPosition(uid: String): JSONObject {
            val header = JSONObject()
            header.put("uid", uid)

            return header
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
    }
}