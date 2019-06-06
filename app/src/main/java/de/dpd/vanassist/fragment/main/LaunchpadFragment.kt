package de.dpd.vanassist.fragment.main

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation

import de.dpd.vanassist.R
import de.dpd.vanassist.util.parcel.ParcelStatus
import kotlinx.android.synthetic.main.fragment_launchpad.view.*
import android.view.animation.AnimationUtils
import android.view.animation.Animation.AnimationListener
import android.widget.Toast
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.util.FragmentRepo

/**
 * Launchpad Fragment
 *
 */
class LaunchpadFragment : Fragment() {

    var dialog:ProgressDialog? = null

    companion object {
        fun newInstance(): LaunchpadFragment {
            return LaunchpadFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_launchpad, container, false)

        if (VanAssistConfig.dayStarted) {
            //Hide start_day button. show back_to_map and finish_day
            v.start_day.visibility = View.INVISIBLE
            v.back_to_map.visibility = View.VISIBLE
            v.finish_day.visibility = View.VISIBLE
        }
        else {
            //Show start_day button. hide back_to_map and finish_day
            v.start_day.visibility = View.VISIBLE
            v.back_to_map.visibility = View.INVISIBLE
            v.finish_day.visibility = View.INVISIBLE
        }

        //Declare Button Listeners
        v.start_day.setOnClickListener {
            val apiController = VanAssistAPIController(activity as AppCompatActivity)

            val courierId = CourierRepository(context!!).getCourierId()
            if(courierId == "474ccac0-92d2-4f3f-8d39-b79557a455f5") {

                if(VanAssistConfig.simulation_running) {
                    val mapFragment = MapFragment.newInstance()

                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                        ?.addToBackStack(FragmentTag.MAP)
                        ?.commit()
                } else {
                    apiController.startSimulation()

                    //apiController.startMapIfSimulationIsRunning(activity as AppCompatActivity)
                    this.dialog = ProgressDialog.show(context, "", getString(R.string.start_simulation___), true)
                    FragmentRepo.launchPadFragment = this
                    VanAssistConfig.dayStarted = true
                }

            } else {
                val mapFragment = MapFragment.newInstance()

                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                    ?.addToBackStack(FragmentTag.MAP)
                    ?.commit()

                VanAssistConfig.dayStarted = true
            }


        }

        v.finish_day.setOnClickListener {
            VanAssistConfig.dayStarted = false
            //Show start_day button. hide back_to_map and finish_day
            //Buttons will fadeOut and then start_btn will fadeIn
            val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.btn_fade_out)

            val courierId = CourierRepository(context!!).getCourierId()
            if(courierId == "474ccac0-92d2-4f3f-8d39-b79557a455f5") {
                val apiController = VanAssistAPIController(activity as AppCompatActivity)
                apiController.stopSimulation()
            }

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

        v.back_to_map.setOnClickListener {

            val mapFragment = MapFragment.newInstance()

            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                ?.addToBackStack(FragmentTag.MAP)
                ?.commit()
        }

        v.open_deliveries.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, ParcelListFragment.newInstance(ParcelStatus.PLANNED), FragmentTag.OPEN_DELIVERY)
                ?.addToBackStack(FragmentTag.OPEN_DELIVERY)
                ?.commit()


        }

        v.delivered_not_delivered.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, TabbedFragment(), FragmentTag.TABBED)
                ?.addToBackStack(FragmentTag.TABBED)
                ?.commit()
        }

        v.settings.setOnClickListener{

            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, SettingsFragment(), FragmentTag.SETTINGS)
                ?.addToBackStack(FragmentTag.SETTINGS)
                ?.commit()
        }

        v.help_and_support.setOnClickListener{

            Toast.makeText(context,getString(R.string.not_available), Toast.LENGTH_LONG).show()

            /*val intent = Intent(context, MainActivity::class.java)
            startActivity(intent) */
        }

        return v
    }
}
