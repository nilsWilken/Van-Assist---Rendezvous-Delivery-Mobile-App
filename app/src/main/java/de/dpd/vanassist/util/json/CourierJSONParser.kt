package de.dpd.vanassist.util.json

import de.dpd.vanassist.database.entity.Courier
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject

class CourierJSONParser {

    companion object {

        fun createHeaderCourierInformationRequest(uid:String, userName:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("user_name", userName)
            return params
        }

        fun createHeaderStartSimulation(uid:String, courierId:String, startTimeInSeconds:Long): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            params.put("start_time_in_seconds", startTimeInSeconds)
            return params

        }

        fun createRequestEmptyBody():JSONObject {
            return JSONObject()
        }

        fun createRequestBodyChangeLanguage(languageCode:String):JSONObject {
            val params = JSONObject()
            params.put("language_code", languageCode)
            return params
        }


        fun parseResponseToCourierObject(response: JSONObject): Courier {
            val status = response.get("status")
            val message = TypeParser.optString(response, "message")
            val data = response.getJSONObject("data")

            val courierId = data.getString("id")
            val firstName = TypeParser.optString(data,"first_name")
            val lastName = TypeParser.optString(data,"last_name")
            val userName = TypeParser.optString(data, "user_name")
            val phoneNumber = TypeParser.optString(data, "phone_number")
            val darkMode = TypeParser.parseIntToBoolean(data.getInt("dark_mode"))
            val mapLabel = TypeParser.parseIntToBoolean(data.getInt("map_label"))
            val languageCode = TypeParser.optString(data,"language_code")
            val verificationToken = ""
            return Courier(
                courierId,
                firstName,
                lastName,
                userName,
                phoneNumber,
                darkMode,
                mapLabel,
                languageCode,
                verificationToken
            )
        }

    }
}