package de.dpd.vanassist.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.fragment.main.MapFragmentOld
import android.view.MotionEvent
import com.mapbox.mapboxsdk.Mapbox
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.MapBoxConfig
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.fragment.main.LaunchpadFragment
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.permission.PermissionHandler
import de.dpd.vanassist.util.language.LanguageManager
import de.dpd.vanassist.intelligence.timeDependentDarkMode.TimeIntentReceiver


@Suppress("DEPRECATION")
class MapActivity : AppCompatActivity() {


    companion object {

        /* Starts the MapActivity
        * -> Can be called from any other activity/fragment */
        fun start(act: AppCompatActivity) {
            val intent = Intent(act, MapActivity::class.java)
            act.startActivity(intent)
            act.finish()
        }
    }


    public override fun onResume() {
        super.onResume()

        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)

        val courier = CourierRepository.shared.getCourier()

        /* Activate darkmode if necessary */
        if (courier?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val apiController = VanAssistAPIController(this)
        /* Load all parcel on startup to check for updates */
        apiController.loadAndSaveAllParcel()

        /* Load all parking areas on startup */
        apiController.getAllParkingLocations()
    }


    override fun onRestart() {
        super.onRestart()

        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)

        val courier = CourierRepository.shared.getCourier()

        /* Activate darkmode if necessary */
        if (courier?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        FragmentRepo.mapActivity = this

        /* Checks information is stored -> if yes, user is logged in
         * -> If no, go to loginActivity
         * -> If yes, check if permissions are granted (if granted, start launchpad, else start Activity to grant permission  */
        if(firebaseUser == null || CourierRepository.shared.getAll().count() != 1) {
            LoginActivity.start(this)
        } else if(!PermissionHandler.permissionGranted(this)) {
            PermissionActivity.start(this)
        } else {
            val apiController = VanAssistAPIController(this)
            if (ParkingAreaRepository.shared.getAll().isEmpty()){
                apiController.getAllParkingLocations()
            }

            startLaunchpadFragment()
        }

        setContentView(R.layout.activity_map)

        Mapbox.getInstance(this, MapBoxConfig.MAP_BOX_ACCESS_TOKEN)

        val courier = CourierRepository.shared.getCourier()

        /* Get and set language code */
        val languageCode = courier?.languageCode
        if(languageCode != null) {
            val locale = LanguageManager.createLocale(languageCode)
            LanguageManager.setLocale(locale, this)
        }

        /* Activate darkmode if necessary */
        if (courier != null && courier.darkMode) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        /* Set time-based darkmode */
        if(courier != null) {
            val trigger = TimeIntentReceiver(this)
            if (courier.ambientIntelligenceMode && courier.timeBasedDarkMode) {
                trigger.cancelAlarmService()
                val themeSwap = trigger.setDarkModeTimes()
                if (themeSwap) {

                    val builder1 = AlertDialog.Builder(this)
                    val apiController = VanAssistAPIController(this)
                    builder1.setTitle(getString(R.string.ai_timeenableddarkmodeTitle))
                    builder1.setMessage(getString(R.string.ai_timeenableddarkmode_message))
                    builder1.setCancelable(true)

                    builder1.setPositiveButton(
                        getString(R.string.yes),
                        DialogInterface.OnClickListener { dialog, id ->
                            dialog.cancel()

                            if (courier.darkMode) {
                                apiController.disableDarkMode()
                            } else {
                                apiController.enableDarkMode()
                            }
                        })

                    builder1.setNegativeButton(
                        getString(R.string.no),
                        DialogInterface.OnClickListener { dialog, id ->
                            //do nothing
                            dialog.cancel()
                        })

                    val alert = builder1.create()
                    alert.show()
                }
            }
            if ((!courier.ambientIntelligenceMode || !courier.timeBasedDarkMode)) {
                trigger.cancelAlarmService()
            }
        }

    }


    private fun startLaunchpadFragment() {
        val launchpadFragment = LaunchpadFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_activity, launchpadFragment, FragmentTag.LAUNCHPAD)
            .commitAllowingStateLoss()
    }


    /* Custom on onBackPress function to handle fragments */
    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val f = supportFragmentManager.findFragmentByTag(FragmentTag.MAP)
            if (f is MapFragmentOld) {
                f.hideBottomSheetFromOutSide(event)
            }
        }
        return super.dispatchTouchEvent(event)
    }

}
