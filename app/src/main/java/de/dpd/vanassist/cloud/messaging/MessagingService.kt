package de.dpd.vanassist.cloud.messaging

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.dpd.vanassist.R
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.fragment.main.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.cloud.CloudMessage
import com.mapbox.geojson.Point
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.fragment.main.VehicleProblemDialogFragment
import kotlinx.android.synthetic.main.activity_map.view.*


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

        if(name == CloudMessage.VAN_STATE) {
            VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, data["latitude"]!!.toDouble(), data["longitude"]!!.toDouble(), data["is_parking"]!!.toBoolean(), data["door_status"]!!, data["logistic_status"]!!, data["problem_status"]!!, data["problem_message"]!!))
        }

        /* Handles response when vehicle is arrived in next parking area */
        if(name == CloudMessage.VEHICLE_IS_IN_NEXT_PARKING_AREA) {
            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!)
            apiController.getCurrentVanState()

            val mapFragment = FragmentRepo.mapFragmentOld
            if(mapFragment != null) {
                if(mapFragment.activity != null) {
                    mapFragment.activity!!.runOnUiThread {
                        Toast.makeText(FragmentRepo.mapFragmentOld!!.context!!, "Vehicle arrived", Toast.LENGTH_LONG).show()
                        val mapFragment = FragmentRepo.mapFragmentOld!!
                        //mapFragment.removeParkingLocationWhenVanHasParked()
                        val destination = mapFragment.destination
                        mapFragment.updateVanLocationWithoutZoom(destination)
                        if(VanRepository.shared.getVanById(VanAssistConfig.VAN_ID) != null) {
                            VanRepository.shared.updateVanLocationById(VanAssistConfig.VAN_ID, destination.latitude(), destination.longitude())
                        }
                        else {
                            VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, destination.latitude(), destination.longitude(), false, "CLOSED", "IN DELIVERY", "OK", ""))
                        }
                        //VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, destination.latitude(), destination.longitude(), true, "CLOSED", "IN DELIVERY", "OK", ""))
                    }
                }
            }
        }


        /* Handles response and updated the current van location on the map */
        if(name == CloudMessage.CURRENT_VAN_LOCATION) {

            val latitude = data[CloudMessage.LATITUDE]!!.toDouble()
            val longitude = data[CloudMessage.LONGITUDE]!!.toDouble()
            val destination = Point.fromLngLat(longitude, latitude)!!

            if(VanRepository.shared.getVanById(VanAssistConfig.VAN_ID) != null) {
                VanRepository.shared.updateVanLocationById(VanAssistConfig.VAN_ID, latitude, longitude)
            }
            else {
                VanRepository.shared.insert(VanEntity(VanAssistConfig.VAN_ID, latitude, longitude, false, "CLOSED", "IN DELIVERY", "OK", ""))
            }
            val mapFragment = FragmentRepo.mapFragmentOld
            if(mapFragment != null) {
                if(mapFragment.activity != null) {
                    mapFragment.activity!!.runOnUiThread {
                        val mapFragment = FragmentRepo.mapFragmentOld!!
                        //mapFragment.addParkingLocationWhenVanStartDriving()
                        mapFragment.updateVanLocationWithoutZoom(destination)
                    }
                }
            }
        }


        if(name == CloudMessage.VAN_PROBLEM) {
            val mapActivity = FragmentRepo.mapActivity
            //mapActivity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.VISIBLE
            //mapActivity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "PROBLEM"

            val apiController = VanAssistAPIController(mapActivity!!)
            apiController.getCurrentVanState()

            val fragmentTransaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
            val prev = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag("van_problem")
            if (prev != null) {
                fragmentTransaction!!.remove(prev)
            }
            val pFragment = VehicleProblemDialogFragment()
            pFragment.show(fragmentTransaction, "van_problem")
        }

    }



}