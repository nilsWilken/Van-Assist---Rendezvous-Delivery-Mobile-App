package de.dpd.vanassist.util.location

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.Log
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import java.lang.Exception

class LocationListeningCallback (fragmentOld: MapFragmentOld) :
    LocationEngineCallback<LocationEngineResult> {

    lateinit var lastLocation: Location
    private val act = fragmentOld

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSuccess(result: LocationEngineResult) {

        // The LocationEngineCallback interface's method which fires when the device's location has changed.

        result.lastLocation
        Log.i("GPS", result.lastLocation.toString())
//        Toast.makeText(
//            act.context
//            , result.lastLocation.toString(), Toast.LENGTH_SHORT
//        ).show()
        lastLocation = result.lastLocation!!

    }

    /**
     * The LocationEngineCallback interface's method which fires when the device's location can not be captured
     *
     * @param exception the exception message
     */
    override fun onFailure(exception: Exception) {

        // The LocationEngineCallback interface's method which fires when the device's location can not be captured
        Log.i("ERROR in callback", "shiiiit")


    }
}