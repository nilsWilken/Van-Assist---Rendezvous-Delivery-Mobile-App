package de.dpd.vanassist.util.json

import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.util.parcel.ParcelListResponseObject
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList

class ParcelJSONParser {

    companion object {

        fun createHeaderGetAllParcelRequest(uid: String, courierId: String, verificationToken:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            params.put("verification_token", verificationToken)
            return params
        }

        fun createHeaderUpdateParcelPosition(uid:String, courierId:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            return params
        }


        fun createRequestBodyUpdateParcelPosition(parcelId:String, newPos:Int): JSONObject {
            val params = JSONObject()
            params.put("parcel_id", parcelId)
            params.put("new_position", newPos)
            return params
        }

        fun createHeaderConfirmDeliveryRequest(uid:String, courierId:String):JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            return params
        }

        fun createRequestEmptyBody():JSONObject {
            return JSONObject()
        }

        fun createBodyConfirmParcelRequest(parcelId:String): JSONObject{
            val params = JSONObject()
            params.put("parcel_id", parcelId)
            return params
        }

        fun parseDeliveryConfirm(response: JSONObject): Parcel {
            val status = response.getInt("status")
            val message = response.getString("message")
            val data = response.getJSONObject("data")

            val id = data.getString("id")
            val state = data.getInt("state")
            val phoneNumber = TypeParser.optString(data, "phone_number")
            val nameOfRecipient = data.getString("name_of_recipient")
            val additionalRecipientInformation = TypeParser.optString(data, "additional_recipient_information")
            val address = data.getString("address")
            val city = data.getString("city")
            val additionalAddressInformation = TypeParser.optString(data,"additional_address_information")
            val deliveryPosition = data.getInt("delivery_position")
            val weight = data.getInt("weight")
            val width = data.getInt("width")
            val height = data.getInt("height")
            val length = data.getInt("length")
            val latitude = data.getString("latitude")
            val longitude = data.getString("longitude")
            val verificationToken = data.getString("verification_token")

            return Parcel(
                id,
                state,
                nameOfRecipient,
                phoneNumber,
                additionalRecipientInformation,
                address,
                city,
                additionalAddressInformation,
                deliveryPosition,
                weight,
                width,
                height,
                length,
                latitude,
                longitude,
                verificationToken
            )
        }


        fun parseResponseToParcelList(response: JSONObject): ParcelListResponseObject {
            val status = response.getInt("status")
            val message = response.getString("message")
            val data = response.getJSONObject("data")
            val parcelListVerificationToken = data.getString("verification_token")

            val parcelResponseList = data.getJSONArray("parcel_list")
            val parcelList = ArrayList<Parcel>()
            for (i in 0..(parcelResponseList.length() - 1)) {
                val parcelJSON = parcelResponseList.getJSONObject(i)

                val id = parcelJSON.getString("id")
                val state = parcelJSON.getInt("state")
                val phoneNumber = TypeParser.optString(parcelJSON,"phone_number")
                val nameOfRecipient = parcelJSON.getString("name_of_recipient")
                val additionalRecipientInformation = TypeParser.optString(parcelJSON,"additional_recipient_information")
                val address = parcelJSON.getString("address")
                val city = parcelJSON.getString("city")
                val additionalAddressInformation = TypeParser.optString(parcelJSON,"additional_address_information")
                val deliveryPosition = parcelJSON.getInt("delivery_position")
                val weight = parcelJSON.getInt("weight")
                val width = parcelJSON.getInt("width")
                val height = parcelJSON.getInt("height")
                val length = parcelJSON.getInt("length")
                val latitude = parcelJSON.getString("latitude")
                val longitude = parcelJSON.getString("longitude")
                val verificationToken = parcelJSON.getString("verification_token")

                val parcel = Parcel(
                    id,
                    state,
                    nameOfRecipient,
                    phoneNumber,
                    additionalRecipientInformation,
                    address,
                    city,
                    additionalAddressInformation,
                    deliveryPosition,
                    weight,
                    width,
                    height,
                    length,
                    latitude,
                    longitude,
                    verificationToken
                )
                parcelList.add(parcel)
            }
            return ParcelListResponseObject(parcelList, parcelListVerificationToken)
        }



        fun parseResponseToParcelListWithoutVerificationToken(response: JSONObject): ArrayList<Parcel> {
            val status = response.getInt("status")
            val message = response.getString("message")

            val parcelResponseList = response.getJSONArray("data")
            val parcelList = ArrayList<Parcel>()
            for (i in 0..(parcelResponseList.length() - 1)) {
                val parcelJSON = parcelResponseList.getJSONObject(i)

                val id = parcelJSON.getString("id")
                val state = parcelJSON.getInt("state")
                val phoneNumber = TypeParser.optString(parcelJSON,"phone_number")
                val nameOfRecipient = parcelJSON.getString("name_of_recipient")
                val additionalRecipientInformation = TypeParser.optString(parcelJSON,"additional_recipient_information")
                val address = parcelJSON.getString("address")
                val city = parcelJSON.getString("city")
                val additionalAddressInformation = TypeParser.optString(parcelJSON,"additional_address_information")
                val deliveryPosition = parcelJSON.getInt("delivery_position")
                val weight = parcelJSON.getInt("weight")
                val width = parcelJSON.getInt("width")
                val height = parcelJSON.getInt("height")
                val length = parcelJSON.getInt("length")
                val latitude = parcelJSON.getString("latitude")
                val longitude = parcelJSON.getString("longitude")
                val verificationToken = parcelJSON.getString("verification_token")

                val parcel = Parcel(
                    id,
                    state,
                    nameOfRecipient,
                    phoneNumber,
                    additionalRecipientInformation,
                    address,
                    city,
                    additionalAddressInformation,
                    deliveryPosition,
                    weight,
                    width,
                    height,
                    length,
                    latitude,
                    longitude,
                    verificationToken
                )
                parcelList.add(parcel)
            }
            return parcelList
        }
    }
}