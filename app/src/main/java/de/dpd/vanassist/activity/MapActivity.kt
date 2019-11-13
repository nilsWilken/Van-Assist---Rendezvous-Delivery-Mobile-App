package de.dpd.vanassist.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.fragment.main.MapFragmentOld
import android.view.MotionEvent
import com.mapbox.mapboxsdk.Mapbox
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.fragment.main.LaunchpadFragment
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.permission.PermissionHandler
import de.dpd.vanassist.util.language.LanguageManager


@Suppress("DEPRECATION")
class MapActivity : AppCompatActivity() {

    companion object {

        fun start(act: AppCompatActivity) {
            val intent = Intent(act, MapActivity::class.java)
            act.startActivity(intent)
            act.finish()
        }

    }

    public override fun onResume() {
        super.onResume()

        val courierRepo = CourierRepository(this)
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val apiController = VanAssistAPIController(this)
        apiController.loadAndSaveAllParcel()
        apiController.getAllParkingLocations()
    }

    override fun onRestart() {
        super.onRestart()
        val courierRepo = CourierRepository(this)
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else{
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        val courierRepo = CourierRepository(this)

        FragmentRepo.mapActivity = this

        val parkingAreaRepo = ParkingAreaRepository(this)

        if(user == null || courierRepo.getAll().count() != 1) {
            LoginActivity.start(this)
        } else if(!PermissionHandler.permissionGranted(this)) {
            PermissionActivity.start(this)
        } else {
            //TODO trigger the parkingArea Loading here
            val apiController = VanAssistAPIController(this)
            if (parkingAreaRepo.getAll().isEmpty()){
                apiController.getAllParkingLocations()
            }

            startLaunchpadFragment()
        }

        setContentView(R.layout.activity_map)

        Mapbox.getInstance(this, VanAssistConfig.MAP_BOX_ACCESS_TOKEN)

        val courier = courierRepo.getCourier()

        val languageCode = courier?.languageCode
        if(languageCode != null) {
            val locale = LanguageManager.createLocale(languageCode)
            LanguageManager.setLocale(locale, this)
        }

//        if (current != null && current.darkMode) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//        }
//        else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//        }

        if (courier != null && courier.darkMode) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }


    private fun startLaunchpadFragment() {
        val launchpadFragment = LaunchpadFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_activity, launchpadFragment, FragmentTag.LAUNCHPAD)
            .commitAllowingStateLoss()
    }

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
