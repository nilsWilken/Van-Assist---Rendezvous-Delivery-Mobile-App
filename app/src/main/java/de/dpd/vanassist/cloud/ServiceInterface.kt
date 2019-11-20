package de.dpd.vanassist.cloud

import org.json.JSONObject

/* Created by Jasmin Weimüller
 * Abstracts from Service Interface (right now Volley Framework)
 * Set Completion Handlers as wished
 * Set Headers and Body
 * Inspired by https://www.varvet.com/blog/kotlin-with-volley */
interface ServiceInterface {

    fun get(path: String, header: JSONObject, completionHandler: (response: JSONObject?) -> Unit)

    fun put(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit)

    fun post(path: String, header: JSONObject, body:JSONObject,  completionHandler: (response: JSONObject?) -> Unit)

    fun delete(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit)

}
