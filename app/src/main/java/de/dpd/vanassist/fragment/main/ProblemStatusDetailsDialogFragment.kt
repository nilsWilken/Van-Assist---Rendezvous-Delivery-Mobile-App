package de.dpd.vanassist.fragment.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.util.FragmentRepo
import kotlinx.android.synthetic.main.fragment_vehicle_problem_details.view.*
import kotlinx.android.synthetic.main.fragment_vehicle_status.view.*
import java.util.*

class ProblemStatusDetailsDialogFragment : Fragment() {

    private lateinit var api: VanAssistAPIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vehicle_problem_details, container, false)

        val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)

        v.goto_vehicle_status_from_vehicle_problem_details_menu.setOnClickListener {
            activity?.onBackPressed()
        }

        v.findViewById<TextView>(R.id.problem_details_van_id_value_text_view)!!.text = van!!.id

        var messageBuilder = StringBuilder(van!!.problemMessage)
        var x = 40
        var index = 0
        while(x < messageBuilder.length) {
            index = messageBuilder.indexOf(" ", x)
            if (index < 0) {
                break
            }
            messageBuilder.replace(index, index+1, "\n\t")
            x = index + 40
        }
        messageBuilder.insert(0, "\t")

        v.findViewById<TextView>(R.id.problem_details_problem_message_value_text_view)!!.text = messageBuilder.toString()

        v.findViewById<TextView>(R.id.problem_details_problem_position_value_text_view)!!.text = "%.5f".format(van!!.latitude).replace(",", ".") + "; " + "%.5f".format(van!!.latitude).replace(",", ".")

        v.problem_details_set_new_parking_position.setOnClickListener {
            //activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.INVISIBLE
            //activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "OK"

            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!)
            apiController.setProblemSolved()
            activity?.onBackPressed()
        }

        v.problem_details_button_continue_mission.setOnClickListener {
            //activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.INVISIBLE
            //activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "OK"

            val mapActivity = FragmentRepo.mapActivity
            val apiController = VanAssistAPIController(mapActivity!!)
            apiController.setProblemSolved()
            activity?.onBackPressed()
        }


        return v
    }

    /*override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)

        val builder1 = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder1.setTitle("Vehicle problem details")
        builder1.setMessage(van!!.problemMessage)
        builder1.setCancelable(true)

        /* Builds the dialog to ask if the courier wants to log out */
        builder1.setPositiveButton(
            "Problem solved",
            DialogInterface.OnClickListener { _, _ ->
                /*val intent = Intent(this.activity, LoginActivity::class.java)
                FirebaseAuth.getInstance().signOut()
                CourierRepository.shared.deleteAll()
                ParcelRepository.shared.deleteAll()
                ParkingAreaRepository.shared.deleteAll()
                SimulationConfig.dayStarted = false
                SimulationConfig.simulation_running = false
                startActivity(intent)
                activity?.finish()*/
                activity?.findViewById<Button>(R.id.button_problem_status_show_details)!!.visibility = View.INVISIBLE
                activity?.findViewById<TextView>(R.id.van_problem_status_value_text_view)!!.text = "OK"

                val mapActivity = FragmentRepo.mapActivity
                val apiController = VanAssistAPIController(mapActivity!!)
                apiController.setProblemSolved()

            })
        builder1.setNegativeButton(
            "Back",
            DialogInterface.OnClickListener {_, _ ->

            }
        )
        builder1.setNeutralButton(
            "Show on map",
            DialogInterface.OnClickListener { _, _ ->
                /* User cancelled the dialog */
            })

        return builder1.create();
    }*/
}