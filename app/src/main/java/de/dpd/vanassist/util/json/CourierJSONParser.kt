package de.dpd.vanassist.util.json

import de.dpd.vanassist.database.entity.CourierEntity
import de.dpd.vanassist.util.TypeParser
import okhttp3.Headers
import org.json.JSONObject

class CourierJSONParser: JSONParser() {

    companion object {

        fun createDefaultHeader(uid:String, courierId:String): JSONObject {
            return JSONParser.createDefaultHeader(uid, courierId)
        }


        fun createHeaderCourierInformationRequest(uid:String, userName:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("user_name", userName)
            return params
        }

        fun createHeaderUpdateFCMToken(uid:String, courierId:String, fcmToken:String): JSONObject {
            val head = JSONObject()
            head.put("uid", uid)
            head.put("courier_id", courierId)
            head.put("fcm_token", fcmToken)

            return head
        }


        fun createRequestBodyChangeLanguage(languageCode:String):JSONObject {
            val params = JSONObject()
            params.put("language_code", languageCode)
            return params
        }


        fun parseResponseToCourierObject(response: JSONObject): CourierEntity {
            println(response)
            val status = response.get("status")
            val message = TypeParser.optString(response, "message")
            val data = response.getJSONObject("data")

            val courierId = data.getString("id")
            val firstName = TypeParser.optString(data,"first_name")
            val lastName = TypeParser.optString(data,"last_name")
            val userName = TypeParser.optString(data, "user_name")
            val phoneNumber = TypeParser.optString(data, "phone_number")
            val darkMode = TypeParser.parseIntToBoolean(data.getInt("dark_mode"))
            val helpMode = TypeParser.parseIntToBoolean(data.getInt("help_mode"))
            val ambientIntelligenceMode = TypeParser.parseIntToBoolean(data.getInt("ambient_intelligence_mode"))
            val intelligentDrivingMode = TypeParser.parseIntToBoolean(data.getInt("intelligent_driving_mode"))
            val timeBasedDarkMode = TypeParser.parseIntToBoolean(data.getInt("time_based_dark_mode"))
            val sizeDependentWaitingMode = TypeParser.parseIntToBoolean(data.getInt("size_dependent_waiting_mode"))
            val dynamicContentMode = TypeParser.parseIntToBoolean(data.getInt("dynamic_content_mode"))
            val gamificationMode = TypeParser.parseIntToBoolean(data.getInt("gamification_mode"))
            val languageCode = TypeParser.optString(data,"language_code")
            val verificationToken = ""
            return CourierEntity(
                courierId,
                firstName,
                lastName,
                userName,
                phoneNumber,
                darkMode,
                helpMode,
                ambientIntelligenceMode,
                intelligentDrivingMode,
                sizeDependentWaitingMode,
                timeBasedDarkMode,
                gamificationMode,
                dynamicContentMode,
                languageCode,
                verificationToken
            )
        }

    }
}