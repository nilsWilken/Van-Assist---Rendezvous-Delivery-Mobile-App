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


class PermissionFragment : Fragment() {

    companion object {

        fun newInstance(): PermissionFragment {
            return PermissionFragment()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_permission, container, false)

        val act = activity as AppCompatActivity

        val firstName = CourierRepository.shared.getCourier()?.firstName

        val permissionText = v.permission_fragment_text as TextView
        permissionText.text = getString(R.string.permission_part_1) + " " + firstName + getString(R.string.permission_part_2)

        val permissionButton = v.permission_fragment_notification_button as Button
        permissionButton.setOnClickListener {
            grantPermission(act)
        }
        return v
    }


    private fun grantPermission(activity:AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= 23) {
            activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,  Manifest.permission.CALL_PHONE),100)
        }
    }
}
