package de.dpd.vanassist.cloud

import org.json.JSONObject

/* Created by Jasmin Weimüller
 * Defines the Service Interface
 * Inspired by https://www.varvet.com/blog/kotlin-with-volley/ */
class APIController constructor(serviceInjection: ServiceInterface): ServiceInterface {

    override fun put(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.put(path, header, body, completionHandler)
    }

    override fun delete(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.delete(path, header, body, completionHandler)
    }

    override fun get(path: String, header: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.get(path, header, completionHandler)
    }

    private val service: ServiceInterface = serviceInjection

    override fun post(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.post(path, header, body, completionHandler)
    }
}