package de.dpd.vanassist.fragment.main

import android.app.ProgressDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation

import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.fragment_launchpad.view.*
import android.view.animation.AnimationUtils
import android.view.animation.Animation.AnimationListener
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.Vehicle
import de.dpd.vanassist.util.parcel.ParcelState

/* Launchpad Fragment */
class LaunchpadFragment : androidx.fragment.app.Fragment() {

    var dialog:ProgressDialog? = null

    companion object {
        fun newInstance(): LaunchpadFragment {
            return LaunchpadFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        (activity as AppCompatActivity).supportActionBar?.title = "VanAssist"

        val v = inflater.inflate(R.layout.fragment_launchpad, container, false)

        if (SimulationConfig.dayStarted) {
            /* Hide start_day button. show back_to_map and finish_day */
            v.start_day.visibility = View.INVISIBLE
            v.back_to_map.visibility = View.VISIBLE
            v.finish_day.visibility = View.VISIBLE
        } else {
            /* Show start_day button. hide back_to_map and finish_day */
            v.start_day.visibility = View.VISIBLE
            v.back_to_map.visibility = View.INVISIBLE
            v.finish_day.visibility = View.INVISIBLE
        }

        /* Declare Button Listeners */
        v.start_day.setOnClickListener {
            val apiController = VanAssistAPIController(activity as AppCompatActivity, requireContext())

            if(SimulationConfig.simulation_running) {
                val mapFragment = MapFragmentOld.newInstance()

                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                    ?.addToBackStack(FragmentTag.MAP)
                    ?.commit()
            } else {
                apiController.startSimulation()
                this.dialog = ProgressDialog.show(context, "", getString(R.string.start_simulation___), true)
                FragmentRepo.launchPadFragment = this
                SimulationConfig.dayStarted = true
            }
        }


        v.finish_day.setOnClickListener {
            SimulationConfig.dayStarted = false
            /* Show start_day button. hide back_to_map and finish_day
             * Buttons will fadeOut and then start_btn will fadeIn */
            val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.btn_fade_out)

            val apiController = VanAssistAPIController(activity as AppCompatActivity, requireContext())
            apiController.stopSimulation()

            val fadeIn = AnimationUtils.loadAnimation(activity, R.anim.btn_fade_in)

            fadeOut.setAnimationListener(object : AnimationListener {

                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation) {
                    v.back_to_map.visibility = View.INVISIBLE
                    v.finish_day.visibility = View.INVISIBLE
                    v.start_day.startAnimation(fadeIn)
                }
            })

            fadeIn.setAnimationListener(object : AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    v.start_day.visibility = View.VISIBLE

                }

                override fun onAnimationStart(animation: Animation?) {}
            })

            v.back_to_map.startAnimation(fadeOut)
            v.finish_day.startAnimation(fadeOut)
        }

        /* Handles click on Back To Map Button */
        v.back_to_map.setOnClickListener {

            val mapFragment = MapFragmentOld.newInstance()

            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                ?.addToBackStack(FragmentTag.MAP)
                ?.commit()
        }

        /* Handles click on Open Delivery Button */
        v.open_deliveries.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, ParcelListFragment.newInstance(ParcelState.PLANNED), FragmentTag.OPEN_DELIVERY)
                ?.addToBackStack(FragmentTag.OPEN_DELIVERY)
                ?.commit()
        }

        /* Handles click on Delivered/Not Delivered Button */
        v.delivered_not_delivered.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, TabbedFragment(), FragmentTag.TABBED)
                ?.addToBackStack(FragmentTag.TABBED)
                ?.commit()
        }

        /* Handles click on Settings Button */
        v.settings.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, SettingsFragment(), FragmentTag.SETTINGS)
                ?.addToBackStack(FragmentTag.SETTINGS)
                ?.commit()
        }

        /* Handles click on Ambient Intelligence Button */
        v.ambient_intelligence.setOnClickListener{

            //activity
            //    ?.supportFragmentManager
            //    ?.beginTransaction()
            //    ?.replace(R.id.map_activity, IntelligenceFragment(), FragmentTag.INTELLIGENCE)
            //    ?.addToBackStack(FragmentTag.INTELLIGENCE)
            //    ?.commit()

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, VehicleStatusFragment(), FragmentTag.VEHICLE_STATUS)
                ?.addToBackStack(FragmentTag.VEHICLE_STATUS)
                ?.commit()
        }

        return v
    }
}
