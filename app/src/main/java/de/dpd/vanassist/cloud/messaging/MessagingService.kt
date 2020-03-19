package de.dpd.vanassist.cloud.messaging

import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.dpd.vanassist.R
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.fragment.main.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.cloud.CloudMessage
import com.mapbox.geojson.Point
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository


class MessagingService: FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        val data = remoteMessage!!.data
        val name = data["name"]

        /* Handles response when simulation is successfully started */
        if(name == CloudMessage.SIMULATION_START) {
            if(FragmentRepo.launchPadFragment != null) {

                SimulationConfig.isFirstVanLocationAfterSimulationStart = true

                val launchpadFragment = FragmentRepo.launchPadFragment
                launchpadFragment!!.dialog!!.dismiss()
                SimulationConfig.simulation_running = true
                val mapFragment = MapFragmentOld.newInstance()
                launchpadFragment.activity!!.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                    ?.addToBackStack(FragmentTag.MAP)
                    ?.commit()
            }
        }

        /* Handles response when simulation is successfully stopped */
        if (name == CloudMessage.SIMULATION_STOP) {
            SimulationConfig.simulation_running = false
        }

        /* Handles response when vehicle is arrived in next parking area */
        if(name == CloudMessage.VEHICLE_IS_IN_NEXT_PARKING_AREA) {
            val mapFragment = FragmentRepo.mapFragmentOld
            if(mapFragment != null) {
                if(mapFragment.activity != null) {
                    mapFragment.activity!!.runOnUiThread {
                        Toast.makeText(FragmentRepo.mapFragmentOld!!.context!!, "Vehicle arrived", Toast.LENGTH_LONG).show()
                        val mapFragment = FragmentRepo.mapFragmentOld!!
                        //mapFragment.removeParkingLocationWhenVanHasParked()
                        val destination = mapFragment.destination
                        mapFragment.updateVanLocationWithoutZoom(destination, true)

                        VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, destination.latitude(), destination.longitude(), true))
                    }
                }
            }
        }


        /* Handles response and updated the current van location on the map */
        if(name == CloudMessage.CURRENT_VAN_LOCATION) {

            val latitude = data[CloudMessage.LONGITUDE]!!.toDouble()
            val longitude = data[CloudMessage.LATITUDE]!!.toDouble()
            val destination = Point.fromLngLat(longitude, latitude)!!

            VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, latitude, longitude, false))
            val mapFragment = FragmentRepo.mapFragmentOld
            if(mapFragment != null) {
                if(mapFragment.activity != null) {
                    mapFragment.activity!!.runOnUiThread {
                        val mapFragment = FragmentRepo.mapFragmentOld!!
                        //mapFragment.addParkingLocationWhenVanStartDriving()
                        mapFragment.updateVanLocationWithoutZoom(destination, false)
                    }
                }
            }
        }

    }



}