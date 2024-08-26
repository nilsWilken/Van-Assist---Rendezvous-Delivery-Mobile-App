package de.dpd.vanassist.fragment.main.launchpad

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.LoginActivity
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.FragmentRepo

class VehicleProblemDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder1 = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder1.setTitle(R.string.vehicle_problem_encountered)
        builder1.setMessage(R.string.vehicle_problem_encountered_message)
        builder1.setCancelable(true)

        /* Builds the dialog to ask if the courier wants to log out */
        builder1.setPositiveButton(
            "OK",
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

                //Acknowledge Error Status
                if(VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                    val mapActivity = FragmentRepo.mapActivity
                    val api = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)
                    api.acknowledgeError()
                }
            })

        builder1.setNegativeButton(
            "Cancel",
            DialogInterface.OnClickListener { _, _ ->
                /* User cancelled the dialog */

                //Reject Error Status
                if(VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                    val mapActivity = FragmentRepo.mapActivity
                    val api = VanAssistAPIController(mapActivity!!, mapActivity.applicationContext)
                    api.rejectError()
                }
            })

        return builder1.create();
    }
}