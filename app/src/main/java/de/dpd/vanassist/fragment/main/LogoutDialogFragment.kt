package de.dpd.vanassist.fragment.main

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment

import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.LoginActivity
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.AppDatabase

class LogoutDialogFragment : androidx.fragment.app.DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val appDatabase = AppDatabase.getDatabase(activity!!.applicationContext)
        val courierDao = appDatabase.courierDao()
        val parcelDao = appDatabase.parcelDao()
        val parkingAreaDao = appDatabase.parkingAreaDao()

        val builder1 = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder1.setTitle(getString(R.string.logout_alert_title))
        builder1.setMessage(getString(R.string.logout_alert_message))
        builder1.setCancelable(true)


        builder1.setPositiveButton(
            getString(R.string.yes),
            DialogInterface.OnClickListener { _, _ ->
                // log out
                val intent = Intent(this.activity, LoginActivity::class.java)
                FirebaseAuth.getInstance().signOut()
                courierDao.deleteAllFromTable()
                parcelDao.deleteAllFromTable()
                parkingAreaDao.deleteAllFromTable()
                VanAssistConfig.dayStarted = false
                VanAssistConfig.simulation_running = false
                startActivity(intent)
                activity?.finish()
            })

        builder1.setNegativeButton(
            getString(R.string.no),
            DialogInterface.OnClickListener { _, _ ->
                // User cancelled the dialog
                })


        return builder1.create();
    }
}

