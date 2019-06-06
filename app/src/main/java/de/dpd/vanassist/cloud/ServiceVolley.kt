package de.dpd.vanassist.cloud

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import de.dpd.vanassist.config.Path
import org.json.JSONObject
import java.util.HashMap


/**
 * Created by Jasmin Weimüller
 * Handles the four essential HTTP Methods: get, post, put, delete
 * Set Headers and Body
 * Inspired by https://www.varvet.com/blog/kotlin-with-volley/
 */
class ServiceVolley : ServiceInterface {
    val TAG = ServiceVolley::class.java.simpleName
    val basePath = Path.BASE_PATH

    override fun post(path: String, header: JSONObject, body: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.POST, basePath + path, body,
                Response.Listener<JSONObject> { response ->
                    Log.d(TAG, "/post request OK! Response: $response")
                    completionHandler(response)
                },
                Response.ErrorListener { error ->
                    Log.d("LOCATION", "/post request fail! Error: ${error.message}")
                    Log.d("LOCATION", "Error: " + {error})
                    completionHandler(null)
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                headers.put("uid", header.getString("uid"))
                return headers
            }
        }

        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }

    override fun get(path: String, header: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
         val jsonObjReq = object : JsonObjectRequest(Method.GET, basePath + path, header,
                Response.Listener<JSONObject> { response ->
                    Log.d(TAG, "/get request OK! Response: $response")
                    completionHandler(response)
                },
                Response.ErrorListener { error ->
                    VolleyLog.e(TAG, "/get request fail! Error: ${error.message}")
                    Log.d("ERROR", "/get request fail! Error: ${error.message}")
                    Log.d("ERROR", "Error: " + {error})

                    completionHandler(null)
                }) {
             @Throws(AuthFailureError::class)
             override fun getHeaders(): Map<String, String> {
                 val headers = HashMap<String, String>()
                 headers.put("Content-Type", "application/json")

                 for (key in header.keys()) {
                     headers.put(key, header.getString(key))
                 }

                 return headers
             }
        }

        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)

    }

    override fun put(path: String, header: JSONObject, body: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.PUT, basePath + path, body,
            Response.Listener<JSONObject> { response ->
                Log.d(TAG, "/put request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/put request fail! Error: ${error.message}")
                completionHandler(null)
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                for (key in header.keys()) {
                    headers.put(key, header.getString(key))
                }
                return headers
            }
        }

        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }

    override fun delete(path: String, header: JSONObject, body:JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
          val jsonObjReq = object : JsonObjectRequest(Method.DELETE, basePath + path, header,
            Response.Listener<JSONObject> { response ->
                Log.d(TAG, "/delete request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/delete request fail! Error: ${error.message}")
                completionHandler(null)
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                for (key in header.keys()) {
                    headers.put(key, header.getString(key))
                }
                return headers
            }
        }

        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }



}