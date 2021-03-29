package de.dpd.vanassist.fragment.main.launchpad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.util.FragmentRepo
import kotlinx.android.synthetic.main.fragment_vehicle_status.*
import kotlinx.android.synthetic.main.fragment_vehicle_status.view.*

class VehicleStatusFragment : Fragment() {

    private lateinit var api: VanAssistAPIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vanObserver = Observer<VanEntity> { van ->
            van_id_value_text_view.text = van!!.id

            van_position_value_text_view.text = "%.5f".format(van!!.latitude).replace(",", ".") + "; " + "%.5f".format(van!!.latitude).replace(",", ".")

            van_doors_value_text_view.text = van!!.doorStatus
            if(van!!.doorStatus == "OPEN") {
                button_open_close_vehicle.text = "CLOSE VAN"
            } else {
                button_open_close_vehicle.text = "OPEN VAN"
            }

            van_logistic_status_value_text_view.text = van!!.logisticStatus
            van_problem_status_value_text_view.text = van!!.problemStatus

            if(van!!.problemStatus == "PROBLEM") {
                button_problem_status_show_details.visibility = View.VISIBLE
            } else {
                button_problem_status_show_details.visibility = View.INVISIBLE
            }
        }

        VanRepository.shared.getVanFlowById(VanAssistConfig.VAN_ID).observe(this, vanObserver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vehicle_status, container, false)

        v.goto_launchpad_from_vehicle_status_menu.setOnClickListener {view ->
            activity?.onBackPressed()
        }

        v.button_problem_status_show_details.setOnClickListener {
            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, ProblemStatusDetailsDialogFragment(), FragmentTag.VEHICLE_PROBLEM_DETAILS)
                ?.addToBackStack(FragmentTag.VEHICLE_PROBLEM_DETAILS)
                ?.commit()
        }

        v.button_test_dialog.setOnClickListener {
            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)
            apiController.sendTestProblem("The van has encountered a critical problem and requires your intervention to continue it's mission. Please head to the vehicle as soon as possible!")
        }

        v.button_open_close_vehicle.setOnClickListener {
            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)

            if(v.findViewById<TextView>(R.id.van_doors_value_text_view)!!.text == "OPEN") {
                apiController.sendDoorStatus("CLOSED")
            } else {
                apiController.sendDoorStatus("OPEN")
            }
            apiController.getCurrentVanState()
        }

        return v
    }

}