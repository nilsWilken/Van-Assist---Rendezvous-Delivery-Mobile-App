package de.dpd.vanassist.fragment.permission

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import de.dpd.vanassist.R
import de.dpd.vanassist.database.repository.CourierRepository
import kotlinx.android.synthetic.main.fragment_permission.view.*
import android.net.Uri
import android.os.Build
import android.provider.Settings


class PermissionFragment : androidx.fragment.app.Fragment() {

    companion object {

        fun newInstance(): PermissionFragment {
            return PermissionFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_permission, container, false)

        val act = activity as AppCompatActivity

        val firstName = CourierRepository(act).getCourier()?.firstName

        val permissionText = v.permission_fragment_text as TextView
        permissionText.text = getString(R.string.permission_part_1) + " " + firstName + getString(R.string.permission_part_2)

        val permissionButton = v.permission_fragment_notification_button as Button
        permissionButton.setOnClickListener {
            grantPermission(act)
            /*if(PermissionHandler.permissionGranted(act)) {
                startMapActivity(act)
            } else {
                Toast.makeText(act, "Please accept permissions", Toast.LENGTH_LONG)
            } */
        }
        return v
    }


    fun startSettings(act:AppCompatActivity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + act.getPackageName()))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        act.startActivity(intent)
    }



    fun grantPermission(activity:AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= 23) {
            activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.CALL_PHONE),100)
        }
    }


/*
    override fun onRequestPermissionsResult(requestCode: Int, permissionsList: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            100 -> {
                if (grantResults.size > 0) {
                    var permissionsDenied = ""
                    requestSend =true
                    for (per in permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per

                        }

                    }
                    // Show permissionsDenied
                    //updateViews()

                    if (permissionsDenied != ""){
                        Log.d("INFO", "All permissions denied");
                        Log.d("INFO", permissionsDenied);

                        showDialogOK("Writing and Location Services Permission required for this app. Do you want to change Permissions",
                            DialogInterface.OnClickListener { dialog, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> runtimePermissions()
                                    DialogInterface.BUTTON_NEGATIVE -> {
                                    }
                                }// proceed with logic by disabling the related features or quit the app.
                            })
                    }
                    else{
                        Log.wtf("Refreshment","TRUE")
                        val currentFragment = this
                        if(currentFragment!=null){
                            val fragmentTransaction = fragmentManager!!.beginTransaction()
                            fragmentTransaction.detach(currentFragment)
                            fragmentTransaction.attach(currentFragment)
                            fragmentTransaction.commit()
                        }

                    }

                    //checkPermissions(applicationContext)

                }
                return
            }
        }
    }
    */
}
