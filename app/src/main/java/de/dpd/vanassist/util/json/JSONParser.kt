package de.dpd.vanassist.util.json

import org.json.JSONObject

open class JSONParser {

    companion object {

        fun createDefaultHeader(uid:String, courierId:String): JSONObject {
            val params = JSONObject()
            params.put("uid", uid)
            params.put("courier_id", courierId)
            return params
        }
    }
}