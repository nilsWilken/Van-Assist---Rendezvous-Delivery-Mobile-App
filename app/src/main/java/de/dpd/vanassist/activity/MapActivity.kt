package de.dpd.vanassist.activity

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl
import de.dpd.vanassist.combox.ConnectionStatus
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.AppDatabase
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.fragment.main.launchpad.LaunchpadFragment
import de.dpd.vanassist.fragment.main.launchpad.VehicleProblemDialogFragment
import de.dpd.vanassist.fragment.main.logisticView.LogisticLaunchpadFragment
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.json.VehicleJSONParser
import de.dpd.vanassist.util.language.LanguageManager
import de.dpd.vanassist.util.permission.PermissionHandler
import de.dpd.vanassist.util.toast.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


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

    private lateinit var bluetoothLeService: BluetoothLeServiceImpl
    private lateinit var bluetoothLeDeliveryService: BluetoothLeDeliveryService

    private var vehicleStatusFlow: Flow<ShortArray>? = null
    private var vehiclePositionFlow: Flow<DoubleArray>? = null
    private var vehicleTargetFlow: Flow<DoubleArray>? = null
    private var errorMessageFlow: Flow<String>? = null
    private var logisticStatusFlow: Flow<Short?>? = null

    public override fun onResume() {
        super.onResume()

        /* Initial Creation of the local Database */
        AppDatabase.createInstance(this)

        val courier = CourierRepository.shared.getCourier()

        /* Activate darkmode if necessary */
        if (courier?.darkMode!!) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val apiController = VanAssistAPIController(this, this.applicationContext)
        /* Load all parcel on startup to check for updates */
        apiController.loadAndSaveAllParcel()

        apiController.getCurrentVanState()

        apiController.updateFCMToken()

        /* Load all parking areas on startup */
        apiController.getAllParkingLocations()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothLeService = BluetoothLeServiceImpl.getInstance(this.applicationContext)
        bluetoothLeDeliveryService = BluetoothLeDeliveryService.getInstance(bluetoothLeService)

        val apiController = VanAssistAPIController(this, this.applicationContext)


        if(VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
            MainScope().launch(Dispatchers.IO) {
                try {
                    apiController.bluetoothLogin(true)

                    vehicleStatusFlow = bluetoothLeDeliveryService.getVehicleStatusNotification()
                    vehiclePositionFlow = bluetoothLeDeliveryService.getVehiclePositionNotification()
                    vehicleTargetFlow = bluetoothLeDeliveryService.getTargetPositionNotification()
                    errorMessageFlow = bluetoothLeDeliveryService.getErrorMessageNotification()
                    logisticStatusFlow = bluetoothLeDeliveryService.getLogisticStatusNotification()


                }catch (e: Exception) {
                    Toast.createToast("Getting notifications failed!")
                }
            }

            MainScope().launch(Dispatchers.IO) {
                try {
                    vehicleStatusFlow!!.collect {
                        VanRepository.shared.updateVanLogisticStatusById(VanAssistConfig.VAN_ID, VehicleJSONParser.parseVehicleStatusFromShort(it[0]))
                    }
                }catch (e: Exception) {
                    Toast.createToast("Collection of vehicle status flow failed!")
                }
            }

            MainScope().launch(Dispatchers.IO) {
                try {
                    vehiclePositionFlow!!.collect {
                        VanRepository.shared.updateVanLocationById(VanAssistConfig.VAN_ID, it[0], it[1])
                    }
                }catch (e: Exception) {
                    Toast.createToast("Collection of vehicle position flow failed!")
                }
            }

            MainScope().launch(Dispatchers.IO) {
                try {

                }catch (e: Exception) {
                    Toast.createToast("Collection of vehicle target flow failed!")
                }
            }

            MainScope().launch(Dispatchers.IO) {
                try {
                    errorMessageFlow!!.collect {
                        VanRepository.shared.updateVanProblemMessageById(VanAssistConfig.VAN_ID, it)
                    }
                }catch (e: Exception) {
                    Toast.createToast("Collection of vehicle status flow failed!")
                }
            }

            MainScope().launch(Dispatchers.IO) {
                try {
                    logisticStatusFlow!!.collect {
                        VanRepository.shared.updateVanLogisticStatusById(VanAssistConfig.VAN_ID, VehicleJSONParser.parseLogisticStatusFromShort(it!!))
                    }
                }catch (e: Exception) {
                    Toast.createToast("Collection of logistic status flow failed!")
                }
            }

            val vanObserver = Observer<VanEntity> { van ->
                if(van!!.problemMessage != "") {
                    val fragmentTransaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
                    val prev = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag("van_problem")
                    if (prev != null) {
                        fragmentTransaction!!.remove(prev)
                    }
                    val pFragment = VehicleProblemDialogFragment()
                    pFragment.show(fragmentTransaction!!, "van_problem")
                }
            }

            VanRepository.shared.getVanFlowById(VanAssistConfig.VAN_ID).observe(this, vanObserver)
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser

        FragmentRepo.mapActivity = this

        /* Checks information is stored -> if yes, user is logged in
         * -> If no, go to loginActivity
         * -> If yes, check if permissions are granted (if granted, start launchpad, else start Activity to grant permission  */
        if ((firebaseUser == null && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) || CourierRepository.shared.getAll().count() != 1) {
            LoginActivity.start(this)
        } else if (!PermissionHandler.permissionGranted(this)) {
            PermissionActivity.start(this)
        } else if(VanAssistConfig.USE_LOGISTICS_LAUNCHPAD) {
            //val apiController = VanAssistAPIController(this, this.applicationContext)
            apiController.getCurrentVanState()
            startLogisticsLaunchpadFragment()
        } else {
            //val apiController = VanAssistAPIController(this, this.applicationContext)
            if (ParkingAreaRepository.shared.getAll().isEmpty()) {
                apiController.getAllParkingLocations()
            }

            apiController.getCurrentVanState()

            //if (VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                //apiController.bluetoothLogin(true)
            //}

            startLaunchpadFragment()
        }

        setContentView(R.layout.activity_map)

        val courier = CourierRepository.shared.getCourier()

        /* Get and set language code */
        val languageCode = courier?.languageCode
        if (languageCode != null) {
            val locale = LanguageManager.createLocale(languageCode)
            LanguageManager.setLocale(locale, this)
        }

        /* Activate darkmode if necessary */
    /*    if (courier != null && courier.darkMode) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    */
        /* Set time-based darkmode */
    /*    if (courier != null) {
            val trigger = TimeIntentReceiver(this)
            if (courier.ambientIntelligenceMode && courier.timeBasedDarkMode) {
                trigger.cancelAlarmService()
                val themeSwap = trigger.setDarkModeTimes()
                if (themeSwap) {

                    val apiController = VanAssistAPIController(this, this.applicationContext)
                    AlertDialog.Builder(this).setTitle(getString(R.string.ai_timeenableddarkmodeTitle))
                        .setMessage(getString(R.string.ai_timeenableddarkmode_message))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes)) { dialog, id ->
                            dialog.cancel()

                            if (courier.darkMode) {
                                apiController.disableDarkMode()
                            } else {
                                apiController.enableDarkMode()
                            }
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, id ->
                            //do nothing
                            dialog.cancel()
                        }
                        .create().show()
                }
            }
            if ((!courier.ambientIntelligenceMode || !courier.timeBasedDarkMode)) {
                trigger.cancelAlarmService()
            }
        }
    */
    }

    override fun onDestroy() {
        super.onDestroy()

        val apiController = VanAssistAPIController(this, this.applicationContext)
        apiController.bluetoothLogout()



    }


    fun startLaunchpadFragment() {
        val launchpadFragment = LaunchpadFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_activity, launchpadFragment, FragmentTag.LAUNCHPAD)
            .commitAllowingStateLoss()
    }

    fun startLaunchpadFragmentWithBackstack() {
        val launchpadFragment = LaunchpadFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_activity, launchpadFragment, FragmentTag.LAUNCHPAD)
            .addToBackStack(FragmentTag.LAUNCHPAD)
            .commit()
    }

    fun startLogisticsLaunchpadFragment() {
        val logisticLaunchpadFragment = LogisticLaunchpadFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_activity, logisticLaunchpadFragment, FragmentTag.LAUNCHPAD)
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
