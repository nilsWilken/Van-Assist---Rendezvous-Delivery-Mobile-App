package de.dpd.vanassist.gps

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.support.annotation.Nullable
import android.util.Log

/**
 * Created by axelherbstreith on 09.03.18.
 */
class GPSService: Service() {

    private var listener: LocationListener? = null
    private var locationManager: LocationManager? = null

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val intent = Intent("location_update")
                intent.putExtra("latitude", location.latitude)
                intent.putExtra("longitude", location.longitude)

                sendBroadcast(intent)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
            }

            override fun onProviderEnabled(s: String) {
            }

            override fun onProviderDisabled(s: String) {
                val i:Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
        }


        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            if(locationManager == null) {
                Log.i("Location Manager", "is null")
            }
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300, 0f, listener)
            Log.wtf("location manager", "was called")
        } catch (se: SecurityException) {
            se.printStackTrace()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("Service", "destroyed")
        try {
            if (locationManager != null) {
                locationManager?.removeUpdates(listener)
            }
        } catch(se: SecurityException) {
            se.printStackTrace()
        }

    }


}