package de.dpd.vanassist.cloud.messaging

import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.dpd.vanassist.R
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.fragment.main.MapFragment
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.cloud.CloudMessage
import com.mapbox.geojson.Point
import de.dpd.vanassist.config.FragmentTag


class MessagingService: FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        val data = remoteMessage!!.data
        val name = data["name"]

        if(name == CloudMessage.SIMULATION_START) {
            if(FragmentRepo.launchPadFragment != null) {

                val launchpadFragment = FragmentRepo.launchPadFragment
                launchpadFragment!!.dialog!!.dismiss()
                VanAssistConfig.simulation_running = true
                val mapFragment = MapFragment.newInstance()
                launchpadFragment.activity!!.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                    ?.addToBackStack(FragmentTag.MAP)
                    ?.commit()
            }
        }

        if(name == CloudMessage.SIMULATION_STOP) {
            VanAssistConfig.simulation_running = false
        }

        if(name == CloudMessage.VEHICLE_IS_IN_NEXT_PARKING_AREA) {
            FragmentRepo.mapFragment!!.activity!!.runOnUiThread {
                Toast.makeText(FragmentRepo.mapFragment!!.context!!, "Vehicle arrived", Toast.LENGTH_LONG).show()
                val mapFragment = FragmentRepo.mapFragment!!
                mapFragment.removeParkingLocationWhenVanHasParked()
                mapFragment.updateVanLocation(mapFragment.destination, mapFragment.mapboxMap.maxZoomLevel - 3)
            }
        }
        

        if(name == CloudMessage.CURRENT_VAN_LOCATION) {

            //TODO Check this --> Longitude to latitude and vice versa
            val latitude = data[CloudMessage.LONGITUDE]!!.toDouble()
            val longitude = data[CloudMessage.LATITUDE]!!.toDouble()
            val destination = Point.fromLngLat(longitude, latitude)!!

            if(FragmentRepo.mapFragment != null) {
                FragmentRepo.mapFragment!!.activity!!.runOnUiThread {
                    val mapFragment = FragmentRepo.mapFragment!!
                    mapFragment.addParkingLocationWhenVanStartDriving()
                    mapFragment.updateVanLocationWithoutZoom(destination)
                }
            }
        }

    }



}