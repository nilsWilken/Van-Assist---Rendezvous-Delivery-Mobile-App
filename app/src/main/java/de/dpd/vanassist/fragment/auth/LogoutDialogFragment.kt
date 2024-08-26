package de.dpd.vanassist.fragment.auth

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.LoginActivity
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository

/* LogOutDialogFragment */
class LogoutDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder1 = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder1.setTitle(getString(R.string.logout_alert_title))
        builder1.setMessage(getString(R.string.logout_alert_message))
        builder1.setCancelable(true)

        /* Builds the dialog to ask if the courier wants to log out */
        builder1.setPositiveButton(
            getString(R.string.yes),
            DialogInterface.OnClickListener { _, _ ->
                val intent = Intent(this.activity, LoginActivity::class.java)
                FirebaseAuth.getInstance().signOut()
                CourierRepository.shared.deleteAll()
                ParcelRepository.shared.deleteAll()
                ParkingAreaRepository.shared.deleteAll()
                SimulationConfig.dayStarted = false
                SimulationConfig.simulation_running = false
                //val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())
                //api.bluetoothLogout()
                startActivity(intent)
                activity?.finish()
            })

        builder1.setNegativeButton(
            getString(R.string.no),
            DialogInterface.OnClickListener { _, _ ->
                /* User cancelled the dialog */
            })

        return builder1.create();
    }
}

