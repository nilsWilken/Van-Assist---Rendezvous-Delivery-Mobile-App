package de.dpd.vanassist.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import de.dpd.vanassist.R
import de.dpd.vanassist.fragment.permission.PermissionFragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.util.permission.PermissionHandler
import de.dpd.vanassist.util.language.LanguageManager


class PermissionActivity : AppCompatActivity() {

    companion object {
        fun start(act: AppCompatActivity) {
            val intent = Intent(act, PermissionActivity::class.java)
            act.startActivity(intent)
            act.finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        val courier = CourierRepository.shared.getCourier()

        /* Set language code */
        val locale = LanguageManager.createLocale(courier?.languageCode!!)
        LanguageManager.setLocale(locale, this)

        /* Activate darkmode if necessary */
        if (courier.darkMode) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        startPermissionFragment()
    }


    public override fun onResume() {
        super.onResume()

        val courier = CourierRepository.shared.getCourier()

        if (courier?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        if(PermissionHandler.permissionGranted(this)) {
            startMapActivity()
        }
    }


    override fun onRestart() {
        super.onRestart()

        val courier = CourierRepository.shared.getCourier()

        if (courier?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }


    private fun startPermissionFragment() {
        val permissionFragment = PermissionFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.activity_permission, permissionFragment, FragmentTag.PERMISSION)
            .commitAllowingStateLoss()
    }

    /* Checks permissions */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100) {
            var allPermissionGranted = true
            for(permissionResult in grantResults) {
                if(permissionResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionGranted = false
                }
            }

            if(allPermissionGranted) {
                startMapActivity()
            }

            var neverAskClicked = false
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    neverAskClicked = true
                }
            }

            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    neverAskClicked = true
                }
            }

            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    neverAskClicked = true
                }
            }

            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
                    neverAskClicked = true
                }
            }

            if(neverAskClicked) {
                showOpenSettingsButton()
            }
        }

    }

    /* Opens android settings */
    private fun startSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun startMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    /* Is executed then permission were denied */
    private fun showOpenSettingsButton() {
        val builder1 = AlertDialog.Builder(this)
        builder1.setTitle(getString(R.string.permission_alert_title))
        builder1.setMessage(getString(R.string.permission_alert_message))
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            getString(R.string.yes)
        ) { dialog, id ->
            startSettings()
            dialog.cancel()
        }

        builder1.setNegativeButton(
            getString(R.string.no)
        ) { dialog, id ->
            dialog.cancel() }

        val alert = builder1.create()
        alert.show()
    }
}
