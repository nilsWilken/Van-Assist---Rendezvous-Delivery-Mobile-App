package de.dpd.vanassist.util.json

import de.dpd.vanassist.database.entity.ParkingAreaEntity

import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList

class ParkingAreaJSONParser: JSONParser() {
    companion object {

        fun createHeaderGetAllParkingAreasRequest(uid: String, courierId: String): JSONObject {
            return createDefaultHeader(uid, courierId)
        }


        fun createBodyPostNextParkingLocation(pA: ParkingAreaEntity): JSONObject {
            val params = JSONObject()
            val parkingArea = JSONObject()
            parkingArea.put("id", pA.id)
            parkingArea.put("edge", pA.edge)
            params.put("parkingArea", parkingArea)
            return params
        }


        fun parseResponseToParkingAreaObject(response: JSONObject): ArrayList<ParkingAreaEntity> {
            val status = response.get("status")
            val message = TypeParser.optString(response, "message")
            val dataList = response.getJSONArray("data")

            val parkingAreaList = ArrayList<ParkingAreaEntity>()
            for (i in 0..(dataList.length() - 1)) {
                val data = dataList.getJSONObject(i)

                val parkingAreaId = data.getString("id")
                val name = TypeParser.optString(data, "name")
                val length = TypeParser.optFloat(data, "length")
                val lane = TypeParser.optString(data, "lane")
                val edge = TypeParser.optString(data, "edge")
                val roadsideCap = TypeParser.optInt(data, "roadsideCapacity")
                val startPos = TypeParser.optFloat(data, "startPos")
                val endPos = TypeParser.optFloat(data, "endPos")
                val lat = TypeParser.optFloat(data, "lat")
                val long = TypeParser.optFloat(data, "long")
                val x = TypeParser.optFloat(data, "x")
                val y = TypeParser.optFloat(data, "y")

                val pA = ParkingAreaEntity(
                    parkingAreaId,
                    name!!,
                    length!!,
                    lane!!,
                    edge!!,
                    roadsideCap!!,
                    startPos!!,
                    endPos!!,
                    lat!!,
                    long!!,
                    x!!,
                    y!!
                )
                parkingAreaList.add(pA)
            }
            return parkingAreaList
        }


        fun parseResponseToParkingAreaObjectSingle(response: JSONObject): ParkingAreaEntity {
            val status = response.get("status")
            val message = TypeParser.optString(response, "message")
            val data = response.getJSONObject("data")

            val parkingAreaId = data.getString("id")
            val name = TypeParser.optString(data, "name")
            val length = TypeParser.optFloat(data, "length")
            val lane = TypeParser.optString(data, "lane")
            val edge = TypeParser.optString(data, "edge")
            val roadsideCap = TypeParser.optInt(data, "roadsideCapacity")
            val startPos = TypeParser.optFloat(data, "startPos")
            val endPos = TypeParser.optFloat(data, "endPos")
            val lat = TypeParser.optFloat(data, "lat")
            val long = TypeParser.optFloat(data, "long")
            val x = TypeParser.optFloat(data, "x")
            val y = TypeParser.optFloat(data, "y")

            val parkingArea = ParkingAreaEntity(
                parkingAreaId,
                name!!,
                length!!,
                lane!!,
                edge!!,
                roadsideCap!!,
                startPos!!,
                endPos!!,
                lat!!,
                long!!,
                x!!,
                y!!
            )
            return parkingArea
        }

    }
}