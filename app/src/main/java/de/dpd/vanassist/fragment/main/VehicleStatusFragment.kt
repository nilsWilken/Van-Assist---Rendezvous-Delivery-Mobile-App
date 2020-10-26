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
import de.dpd.vanassist.util.FragmentRepo
import kotlinx.android.synthetic.main.fragment_vehicle_status.view.*

class VehicleStatusFragment : Fragment() {

    private lateinit var api: VanAssistAPIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vehicle_status, container, false)

        v.goto_launchpad_from_vehicle_status_menu.setOnClickListener {view ->
            activity?.onBackPressed()
        }

        v.button_problem_status_show_details.setOnClickListener {
            val detailDialog = ProblemStatusDetailsDialogFragment()
            detailDialog.show(activity?.supportFragmentManager, "problem_details")
        }

        v.button_test_dialog.setOnClickListener {
            activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.VISIBLE
            activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "PROBLEM"
            val fragmentTransaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
            val prev = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag("van_problem")
            if (prev != null) {
                fragmentTransaction!!.remove(prev)
            }
            val pFragment = VehicleProblemDialogFragment()
            pFragment.show(fragmentTransaction, "van_problem")
            //val problemDialog = VehicleProblemDialogFragment()
            //problemDialog.show(activity?.supportFragmentManager, "vehicle_problem")


        }

        return v
    }

}