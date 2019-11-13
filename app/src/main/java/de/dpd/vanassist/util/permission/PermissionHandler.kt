package de.dpd.vanassist.util.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class PermissionHandler {

    companion object {

        fun permissionGranted(activity:AppCompatActivity): Boolean {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        }


        fun permissionDenied(activity:AppCompatActivity):Boolean {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED
        }




    }
}