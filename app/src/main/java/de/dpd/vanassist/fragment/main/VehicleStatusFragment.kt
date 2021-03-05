package de.dpd.vanassist.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.util.FragmentRepo
import kotlinx.android.synthetic.main.fragment_vehicle_status.view.*
import java.util.*

class VehicleStatusFragment : Fragment() {

    private lateinit var api: VanAssistAPIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vehicle_status, container, false)

        val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)

        v.goto_launchpad_from_vehicle_status_menu.setOnClickListener {view ->
            activity?.onBackPressed()
        }

        v.findViewById<TextView>(R.id.van_id_value_text_view)!!.text = van!!.id
        v.findViewById<TextView>(R.id.van_position_value_text_view)!!.text = "%.5f".format(van!!.latitude).replace(",", ".") + "; " + "%.5f".format(van!!.latitude).replace(",", ".")

        val doorStatus = van!!.doorStatus
        v.findViewById<TextView>(R.id.van_doors_value_text_view)!!.text = van!!.doorStatus
        if(doorStatus!! == "OPEN") {
            v.findViewById<Button>(R.id.button_open_close_vehicle)!!.text = "CLOSE VAN"
        } else {
            v.findViewById<Button>(R.id.button_open_close_vehicle)!!.text = "OPEN VAN"
        }

        v.findViewById<TextView>(R.id.van_logistic_status_value_text_view)!!.text = van!!.logisticStatus
        v.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = van!!.problemStatus
        if(van!!.problemStatus == "PROBLEM") {
            v.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.VISIBLE
        }

        v.button_problem_status_show_details.setOnClickListener {
            //val detailDialog = ProblemStatusDetailsDialogFragment()
            //detailDialog.show(activity?.supportFragmentManager, "problem_details")
            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, ProblemStatusDetailsDialogFragment(), FragmentTag.VEHICLE_PROBLEM_DETAILS)
                ?.addToBackStack(FragmentTag.VEHICLE_PROBLEM_DETAILS)
                ?.commit()
        }

        v.button_test_dialog.setOnClickListener {
            //activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.VISIBLE
            //activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "PROBLEM"
            //val fragmentTransaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
            //val prev = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag("van_problem")
            //if (prev != null) {
            //    fragmentTransaction!!.remove(prev)
            //}
            //val pFragment = VehicleProblemDialogFragment()
            //pFragment.show(fragmentTransaction, "van_problem")
            //val problemDialog = VehicleProblemDialogFragment()
            //problemDialog.show(activity?.supportFragmentManager, "vehicle_problem")

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