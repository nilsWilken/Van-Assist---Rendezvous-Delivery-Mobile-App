package de.dpd.vanassist.fragment.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.fragment_vehicle_status.view.*

class ProblemStatusDetailsDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder1 = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder1.setTitle("Vehicle problem details")
        builder1.setMessage("The vehicle has encountered an unsolvable problem!")
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
    }
}