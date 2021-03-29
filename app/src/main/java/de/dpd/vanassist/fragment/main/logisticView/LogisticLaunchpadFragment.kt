package de.dpd.vanassist.fragment.main.logisticView

import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.fragment.main.TabbedFragment
import de.dpd.vanassist.fragment.main.launchpad.ParcelListFragment
import de.dpd.vanassist.fragment.main.launchpad.SettingsFragment
import de.dpd.vanassist.fragment.main.launchpad.VehicleStatusFragment
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.parcel.ParcelState
import kotlinx.android.synthetic.main.fragment_launchpad.view.*
import kotlinx.android.synthetic.main.fragment_logistics_launchpad.*
import kotlinx.android.synthetic.main.fragment_logistics_launchpad.view.*
import kotlinx.android.synthetic.main.fragment_vehicle_status.*

class LogisticLaunchpadFragment: androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance(): LogisticLaunchpadFragment {
            return LogisticLaunchpadFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vanObserver = Observer<VanEntity> { van ->
            logistics_launchpad_id_value.text = van!!.id

            logistics_launchpad_position_value.text = "%.5f".format(van!!.latitude).replace(",", ".") + "; " + "%.5f".format(van!!.latitude).replace(",", ".")

            logistics_launchpad_door_status_value.text = van!!.doorStatus

            logistics_launchpad_logisticStatus_value.text = van!!.logisticStatus

            logistics_launchpad_button_set_available.isClickable = false
            logistics_launchpad_button_set_available.background.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)

            logistics_launchpad_button_set_loading.isClickable = false
            logistics_launchpad_button_set_loading.background.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)

            //logistics_launchpad_button_set_ready.isClickable = false
            //logistics_launchpad_button_set_ready.background.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
        }

        VanRepository.shared.getVanFlowById(VanAssistConfig.VAN_ID).observe(this, vanObserver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        (activity as AppCompatActivity).supportActionBar?.title = "VanAssist"

        val v = inflater.inflate(R.layout.fragment_logistics_launchpad, container, false)


        /* Declare Button Listeners */
        v.logistics_launchpad_button_set_available.setOnClickListener {
            val apiController = VanAssistAPIController(activity as AppCompatActivity, requireContext())

        }

        v.logistics_launchpad_button_set_loading.setOnClickListener {

        }

        v.logistics_launchpad_button_set_ready.setOnClickListener {

        }

        return v
    }
}