package de.dpd.vanassist.util.json

import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.util.parcel.ParcelListResponseObject
import de.dpd.vanassist.util.TypeParser
import org.json.JSONObject
import java.util.ArrayList

class ParcelJSONParser: JSONParser() {

    companion object {

        fun createHeaderGetAllParcelRequest(uid: String, courierId: String, verificationToken:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            params.put("verification_token", verificationToken)
            return params
        }


        fun createHeaderUpdateParcelPosition(uid:String, courierId:String): JSONObject {
            return createDefaultHeader(uid, courierId)
        }


        fun createRequestBodyUpdateParcelPosition(parcelId:String, newPos:Int): JSONObject {
            val params = JSONObject()
            params.put("parcel_id", parcelId)
            params.put("new_position", newPos)
            return params
        }


        fun createHeaderConfirmDeliveryRequest(uid:String, courierId:String):JSONObject {
            return createDefaultHeader(uid, courierId)
        }


        fun createRequestEmptyBody():JSONObject {
            return JSONObject()
        }


        fun createBodyConfirmParcelRequest(parcelId:String): JSONObject{
            val params = JSONObject()
            params.put("parcel_id", parcelId)
            return params
        }


        fun parseDeliveryConfirm(response: JSONObject): ParcelEntity {
            val status = response.getInt("status")
            val message = response.getString("message")
            val data = response.getJSONObject("data")

            val id = data.getString("id")
            val state = data.getInt("state")
            val phoneNumber = TypeParser.optString(data, "phone_number")
            val nameOfRecipient = data.getString("name_of_recipient")
            val additionalRecipientInformation = TypeParser.optString(data, "additional_recipient_information")
            val floor = data.getDouble("floor")
            val address = data.getString("address")
            val city = data.getString("city")
            val additionalAddressInformation = TypeParser.optString(data,"additional_address_information")
            val deliveryPosition = data.getInt("delivery_position")
            val weight = data.getDouble("weight")
            val width = data.getDouble("width")
            val height = data.getDouble("height")
            val length = data.getDouble("length")
            val latitude = data.getString("latitude")
            val longitude = data.getString("longitude")
            val verificationToken = data.getString("verification_token")
            val parkingArea = data.getString("parkingArea")

            return ParcelEntity(
                id,
                state,
                nameOfRecipient,
                phoneNumber,
                additionalRecipientInformation,
                floor,
                city,
                address,
                additionalAddressInformation,
                deliveryPosition,
                weight,
                width,
                height,
                length,
                latitude,
                longitude,
                verificationToken,
                parkingArea
            )
        }


        fun parseResponseToParcelList(response: JSONObject): ParcelListResponseObject {
            val status = response.getInt("status")
            val message = response.getString("message")
            val data = response.getJSONObject("data")
            val parcelListVerificationToken = data.getString("verification_token")

            val parcelResponseList = data.getJSONArray("parcel_list")
            val parcelList = ArrayList<ParcelEntity>()
            for (i in 0..(parcelResponseList.length() - 1)) {
                val parcelJSON = parcelResponseList.getJSONObject(i)

                val id = parcelJSON.getString("id")
                val state = parcelJSON.getInt("state")
                val phoneNumber = TypeParser.optString(parcelJSON,"phone_number")
                val nameOfRecipient = parcelJSON.getString("name_of_recipient")
                val additionalRecipientInformation = TypeParser.optString(parcelJSON,"additional_recipient_information")
                val floor = parcelJSON.getDouble("floor")
                val address = parcelJSON.getString("address")
                val city = parcelJSON.getString("city")
                val additionalAddressInformation = TypeParser.optString(parcelJSON,"additional_address_information")
                val deliveryPosition = parcelJSON.getInt("delivery_position")
                val weight = parcelJSON.getDouble("weight")
                val width = parcelJSON.getDouble("width")
                val height = parcelJSON.getDouble("height")
                val length = parcelJSON.getDouble("length")
                val latitude = parcelJSON.getString("latitude")
                val longitude = parcelJSON.getString("longitude")
                val verificationToken = parcelJSON.getString("verification_token")
                val parkingArea = parcelJSON.getString("parkingArea")

                val parcel = ParcelEntity(
                    id,
                    state,
                    nameOfRecipient,
                    phoneNumber,
                    additionalRecipientInformation,
                    floor,
                    city,
                    address,
                    additionalAddressInformation,
                    deliveryPosition,
                    weight,
                    width,
                    height,
                    length,
                    latitude,
                    longitude,
                    verificationToken,
                    parkingArea
                )
                parcelList.add(parcel)
            }
            return ParcelListResponseObject(parcelList, parcelListVerificationToken)
        }

        fun parseBluetoothParcelResponse(response: JSONObject): ParcelEntity {
            val parcelJSON = response.getJSONObject("parcel")

            val id = parcelJSON.getString("id")
            val state = parcelJSON.getInt("state")
            val phoneNumber = TypeParser.optString(parcelJSON,"phone_number")
            val nameOfRecipient = parcelJSON.getString("name_of_recipient")
            val additionalRecipientInformation = TypeParser.optString(parcelJSON,"additional_recipient_information")
            val floor = parcelJSON.getDouble("floor")
            val address = parcelJSON.getString("address")
            val city = parcelJSON.getString("city")
            val additionalAddressInformation = TypeParser.optString(parcelJSON,"additional_address_information")
            val deliveryPosition = parcelJSON.getInt("delivery_position")
            val weight = parcelJSON.getDouble("weight")
            val width = parcelJSON.getDouble("width")
            val height = parcelJSON.getDouble("height")
            val length = parcelJSON.getDouble("length")
            val latitude = parcelJSON.getString("latitude")
            val longitude = parcelJSON.getString("longitude")
            val verificationToken = parcelJSON.getString("verification_token")
            val parkingArea = parcelJSON.getString("parkingArea")

            val parcel = ParcelEntity(
                id,
                state,
                nameOfRecipient,
                phoneNumber,
                additionalRecipientInformation,
                floor,
                city,
                address,
                additionalAddressInformation,
                deliveryPosition,
                weight,
                width,
                height,
                length,
                latitude,
                longitude,
                verificationToken,
                parkingArea
            )

            return parcel
        }

        fun parseResponseToParcelListWithoutVerificationToken(response: JSONObject): ArrayList<ParcelEntity> {
            val status = response.getInt("status")
            val message = response.getString("message")

            val parcelResponseList = response.getJSONArray("data")
            val parcelList = ArrayList<ParcelEntity>()
            for (i in 0..(parcelResponseList.length() - 1)) {
                val parcelJSON = parcelResponseList.getJSONObject(i)

                val id = parcelJSON.getString("id")
                val state = parcelJSON.getInt("state")
                val phoneNumber = TypeParser.optString(parcelJSON,"phone_number")
                val nameOfRecipient = parcelJSON.getString("name_of_recipient")
                val additionalRecipientInformation = TypeParser.optString(parcelJSON,"additional_recipient_information")
                val floor = parcelJSON.getDouble("floor")
                val address = parcelJSON.getString("address")
                val city = parcelJSON.getString("city")
                val additionalAddressInformation = TypeParser.optString(parcelJSON,"additional_address_information")
                val deliveryPosition = parcelJSON.getInt("delivery_position")
                val weight = parcelJSON.getDouble("weight")
                val width = parcelJSON.getDouble("width")
                val height = parcelJSON.getDouble("height")
                val length = parcelJSON.getDouble("length")
                val latitude = parcelJSON.getString("latitude")
                val longitude = parcelJSON.getString("longitude")
                val verificationToken = parcelJSON.getString("verification_token")
                val parkingArea = parcelJSON.getString("parkingArea")

                val parcel = ParcelEntity(
                    id,
                    state,
                    nameOfRecipient,
                    phoneNumber,
                    additionalRecipientInformation,
                    floor,
                    city,
                    address,
                    additionalAddressInformation,
                    deliveryPosition,
                    weight,
                    width,
                    height,
                    length,
                    latitude,
                    longitude,
                    verificationToken,
                    parkingArea
                )
                parcelList.add(parcel)
            }
            return parcelList
        }
    }
}