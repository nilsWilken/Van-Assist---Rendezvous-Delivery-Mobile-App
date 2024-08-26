package de.dpd.vanassist.cloud
import android.app.Application
import android.text.TextUtils
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.facebook.stetho.Stetho
import de.dpd.vanassist.database.AppDatabase

/* Created by Jasmin Weimüller
 * Backend Handling for the Volley HTTP Completion Framework
 * Inspired by https://www.varvet.com/blog/kotlin-with-volley */
class BackendVolley : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Stetho.initializeWithDefaults(this)
        AppDatabase.createInstance(this)
    }

    private val requestQueue: RequestQueue? = null
        get() {
            if (field == null) {
                return Volley.newRequestQueue(applicationContext)
            }
            return field
        }

    fun <T> addToRequestQueue(request: Request<T>, tag: String) {
        request.tag = if (TextUtils.isEmpty(tag)) TAG else tag
        requestQueue?.add(request)
    }

    companion object {
        private val TAG = BackendVolley::class.java.simpleName
        @get:Synchronized var instance: BackendVolley? = null
            private set
    }
}
