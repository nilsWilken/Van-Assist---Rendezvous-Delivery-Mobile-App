package de.dpd.vanassist.util

import org.json.JSONObject

class TypeParser {

    companion object {
        fun parseIntToBoolean(value:Int):Boolean{
            if(value == 1)
                return true
            return false
        }

        fun optString(json:JSONObject, key:String): String? {
            if(json.isNull(key)) {
                return null
            }
            else {
                return json.getString(key)
            }
        }
        fun optFloat(json:JSONObject, key:String): Float? {
            if(json.isNull(key)) {
                return null
            }
            else {
                return (json.getString(key).toFloat())
            }
        }
        fun optInt(json:JSONObject, key:String): Int? {
            if(json.isNull(key)) {
                return null
            }
            else {
                return json.getInt(key)
            }
        }
    }
}