package de.dpd.vanassist.cloud

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl
import de.dpd.vanassist.combox.ConnectionStatus
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.Path
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.entity.CourierEntity
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.entity.ParkingAreaEntity
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.fragment.main.map.MapFragmentOld
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.date.DateParser
import de.dpd.vanassist.util.json.CourierJSONParser
import de.dpd.vanassist.util.json.ParcelJSONParser
import de.dpd.vanassist.util.json.ParkingAreaJSONParser
import de.dpd.vanassist.util.json.VehicleJSONParser
import de.dpd.vanassist.util.toast.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class VanAssistAPIController(activity: AppCompatActivity, context: Context) {

    private val service = ServiceVolley()
    private val apiController = APIController(service)
    val main = activity

    private val bluetoothLeService = BluetoothLeServiceImpl.getInstance(context)
    private val bluetoothLeDeliveryService = BluetoothLeDeliveryService.getInstance(bluetoothLeService)
    private val bleConnectionStatus: StateFlow<ConnectionStatus> = bluetoothLeService.connectionStatus


    private lateinit var auth: FirebaseAuth

    fun checkConnectionStatus() {
        if(this.bleConnectionStatus.value.description == ConnectionStatus.NotConnected().description) {
            this.bluetoothLeService.connect()
        }
    }

    fun bluetoothLogin(enable: Boolean) {

        MainScope().launch(Dispatchers.Default) {
            Log.i("BLEService", bleConnectionStatus.value.description)

            if(!bluetoothLeService.getServicesDiscovered()) {
                checkConnectionStatus()
            }
            while(!bluetoothLeService.getServicesDiscovered()) {
                Thread.sleep(100)
            }
            Log.i("BLEService", bleConnectionStatus.value.description)

            withContext(Dispatchers.Default){bluetoothLeDeliveryService.writeCredentialsUsername("vanassist@hs-offenburg.de")}
            withContext(Dispatchers.Default){bluetoothLeDeliveryService.writeCredentialsPassword("myPasssword2")}
            withContext(Dispatchers.Default){bluetoothLeDeliveryService.setConnect(if (enable) 1 else 2)}

            /*Log.i("BLEService", bluetoothLeDeliveryService.getVehicleId())
            Log.i("BLEService", bluetoothLeDeliveryService.getDeliveryCount().toString())
            Log.i(BLEService", bluetoothLeDeliveryService.getDeliveryMode().toString())
            bluetoothLeDeliveryService.setVehicleId("TestID")
            Log.i("BLEService", bluetoothLeDeliveryService.getVehicleId())*/
        }
    }

    fun acknowledgeError() {
        MainScope().launch(Dispatchers.IO) {
            bluetoothLeDeliveryService.acknowledgeErrorOrStatus(0,0)
        }
    }

    fun rejectError() {
        MainScope().launch(Dispatchers.IO) {
            bluetoothLeDeliveryService.acknowledgeErrorOrStatus(1, 0)
        }
    }

    fun bluetoothLogout() {
        MainScope().launch(Dispatchers.IO) {
            bluetoothLeService.disconnect()
        }
    }


    /* Created by Axel Herbstreith and Jasmin Weimüller
     * Starts the log in process for the courier
     * Firebase Authentication is handled in this method, if successful courier information will be loaded from server
     *
     * @param activity, username, password
     * @return void */
    fun userAuthentication(act: AppCompatActivity, userName: String, password: String) {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val dialog = ProgressDialog.show(act, "", act.getString(R.string.authenticating___), true)
                auth = FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(userName, password)
                    .addOnCompleteListener(act) { task ->
                        if (task.isSuccessful) {
                            loadAndSaveCourierInformation(act, dialog, userName)
                        } else {
                            dialog.dismiss()
                            android.widget.Toast.makeText(
                                act.baseContext,
                                act.getString(R.string.authentication_failed),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            else {
                val dialog = ProgressDialog.show(act, "", act.getString(R.string.authenticating___), true)
                bluetoothLogin(true)
                loadAndSaveCourierInformation(act, dialog, userName)
            }
        }
        else {
            loadAndSaveCourierInformation(act, null, userName)
        }
    }

    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Load and saves the courier information
     * -> Get the Firebase Token
     * -> Create params for api call
     * Then all parcel information are loaded in background
     * Then, the map Activity is only started when
     *  - the data was retrieved from server, stored on the local database and checked if exactly one user entry is in the local DB
     *  - All permissions are granted, if not -> start Permission Activity
     *
     * @param activity, dialog
     * @return Void
     **/
    private fun loadAndSaveCourierInformation(act: AppCompatActivity, dialog: ProgressDialog?, userName: String) {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {

                            val params = CourierJSONParser.createHeaderCourierInformationRequest(uid, userName)
                            val path = Path.COURIER_INFORMATION

                            apiController.get(path, params) { response ->
                                if (response != null) {
                                    try {
                                        val jsonObject = JSONObject(response.toString())
                                        val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                        CourierRepository.shared.insert(courier)

                                        if (CourierRepository.shared.getAll().size == 1) {
                                            //loadAndSaveAllParcel()
                                            dialog!!.dismiss()
                                            MapActivity.start(act)
                                        }
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_courier_information_loading))
                                }
                            }
                        }
                    } else {
                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_courier_information_loading))
                    }
                }
        }
        else {
            val cInstance = CourierEntity(
                "exampleID",
                "Christian",
                "Mustermann",
                "vanassist@uni-mannheim.de",
                "00125243",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "en_US",
                "exampleVerificationToken"
            )
            CourierRepository.shared.insert(cInstance)

            if(CourierRepository.shared.getAll().size == 1) {
                if(dialog != null) {
                    dialog.dismiss()
                }
                MapActivity.start(act)
            }
        }
    }


    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Requests all parcel information from server, parses the response and stores it on the local database
     *
     * @param
     * @return void
     **/
    fun loadAndSaveAllParcel() {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val courier = CourierRepository.shared.getCourier()!!
                                val courierId = courier.id
                                val verificationToken = courier.verificationToken
                                val params = ParcelJSONParser.createHeaderGetAllParcelRequest(uid, courierId, verificationToken)
                                val path = Path.PARCEL_ALL

                                apiController.get(path, params) { response ->
                                    if (response != null) {
                                        val strResp = response.toString()
                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val parcelResponseObject = ParcelJSONParser.parseResponseToParcelList(jsonObject)
                                            CourierRepository.shared.updateVerificationToken(parcelResponseObject.verificationToken)
                                            ParcelRepository.shared.insertAll(parcelResponseObject.parcelList)
                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_loading))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_loading))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_loading))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        val deliveryCount = async { bluetoothLeDeliveryService.getDeliveryCount()!! }

                        val deliveries: MutableList<String> = ArrayList<String>()
                        for (i in 0..deliveryCount.await()) {
                            //deliveries.add(bluetoothLeDeliveryService.getDeliveryListItem(i.toShort()))
                            try {
                                val deliveryListItem = async { bluetoothLeDeliveryService.getDeliveryListItem(i.toShort()) }
                                ParcelRepository.shared.insert(
                                    ParcelJSONParser.parseBluetoothParcelResponse(
                                        JSONObject(
                                            deliveryListItem.await()
                                        )
                                    )
                                )
                            } catch (e: JSONException) {
                                Toast.createToast("Loading parcel failed!")
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.createToast("Loading parcel list failed!")
                    }
                }
            }
        }
        else {
            //TODO: CREATE DUMMY PARCEL DATA
            if(ParcelRepository.shared.getAll().size == 0) {
                val parcel1 = ParcelEntity(
                    "p1",
                    0,
                    "Max Mustermann",
                    "01567234529",
                    null,
                    0.0,
                    "Heidelberg",
                    "Berliner Straße 41",
                    null,
                    0,
                    10.0,
                    24.0,
                    10.0,
                    20.0,
                    "0.0",
                    "0.0",
                    "abc"
                )
                ParcelRepository.shared.insert(parcel1)

                val parcel2 = ParcelEntity(
                    "p2",
                    0,
                    "Lustig 2 GmbH",
                    null,
                    null,
                    0.0,
                    "Heidelberg",
                    "Berliner Straße 41",
                    null,
                    1,
                    10.0,
                    24.0,
                    10.0,
                    20.0,
                    "0.0",
                    "0.0",
                    "abc"
                )
                ParcelRepository.shared.insert(parcel2)

                val parcel3 = ParcelEntity(
                    "p3",
                    0,
                    "FireAlarm Inc.",
                    null,
                    null,
                    0.0,
                    "Heidelberg",
                    "Berliner Straße 41",
                    null,
                    2,
                    10.0,
                    24.0,
                    10.0,
                    20.0,
                    "0.0",
                    "0.0",
                    "abc"
                )
                ParcelRepository.shared.insert(parcel3)

                val parcel4 = ParcelEntity(
                    "p4",
                    0,
                    "Heidrun Weber",
                    null,
                    null,
                    0.0,
                    "Heidelberg",
                    "Berliner Straße 41",
                    null,
                    3,
                    10.0,
                    24.0,
                    10.0,
                    20.0,
                    "0.0",
                    "0.0",
                    "abc"
                )
                ParcelRepository.shared.insert(parcel4)

                val parcel5 = ParcelEntity(
                    "p5",
                    0,
                    "Yoga Becker",
                    null,
                    null,
                    0.0,
                    "Heidelberg",
                    "Berliner Straße 41",
                    null,
                    4,
                    10.0,
                    24.0,
                    10.0,
                    20.0,
                    "0.0",
                    "0.0",
                    "abc"
                )
                ParcelRepository.shared.insert(parcel5)

            }
            //ParcelRepository.shared.insert(pInstance1)
        }
    }

    /* Starts the simulation
     * This method needs to be executed by another framework than volley (in our case OkHttp3) since there was an issue with using Volley -> see report) */
    fun startSimulation() {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                    val fcmToken = instanceIdResult.token

                    val user = FirebaseAuth.getInstance().currentUser
                    user?.getIdToken(true)!!
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = task.result!!.token
                                if (uid != null) {

                                    val courierId = CourierRepository.shared.getCourierId()!!
                                    val url = Path.BASE_PATH + Path.SIMULATION_START
                                    val client = OkHttpClient()
                                    val secondsSinceMidnight = DateParser.getSecondsSinceMidnight().toString()

                                    val head = mutableMapOf<String, String>()
                                    head.put("uid", uid)
                                    head.put("courier_id", courierId)
                                    head.put("seconds_since_midnight", secondsSinceMidnight)
                                    head.put("fcm_token", fcmToken)
                                    val headerBuild = Headers.of(head)

                                    val request = Request.Builder().url(url).headers(headerBuild).build()



                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            e.printStackTrace();
                                        }

                                        @Throws(IOException::class)
                                        override fun onResponse(call: Call, response: Response) {
                                            if (response.isSuccessful) {
                                                if (response != null) {
                                                    val strResp = response.body()!!.string()
                                                    try {
                                                        val jsonObject = JSONObject(strResp)
                                                        Log.wtf("Test;", strResp)
                                                        val data = jsonObject.getJSONObject("data")
                                                        val simulationIsRunning = data.getBoolean("simulation_is_running")

                                                        if (simulationIsRunning) {

                                                            FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                                                            val mapFragment = MapFragmentOld.newInstance()
                                                            FragmentRepo.launchPadFragment!!.requireActivity().supportFragmentManager
                                                                ?.beginTransaction()
                                                                ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                                                                ?.addToBackStack(FragmentTag.MAP)
                                                                ?.commit()
                                                        }

                                                    } catch (e: JSONException) {
                                                        println(e.printStackTrace())
                                                        FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_start_simulation))
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_start_simulation))
                                                }
                                            } else {
                                                FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_start_simulation))
                                            }
                                        }
                                    })
                                }
                            }
                        }
                }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }
                        withContext(Dispatchers.IO) { bluetoothLeDeliveryService.startDelivery() }

                        FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                        val mapFragment = MapFragmentOld.newInstance()
                        FragmentRepo.launchPadFragment!!.requireActivity().supportFragmentManager
                            ?.beginTransaction()
                            ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                            ?.addToBackStack(FragmentTag.MAP)
                            ?.commit()
                    } catch (e: Exception) {
                        Toast.createToast("Starting delivery failed!")
                    }
                }
            }
        } else {
            //TODO: WHAT HAPPENS IN DEMO SCENARIO?
            val mapFragment = MapFragmentOld.newInstance()
            FragmentRepo.launchPadFragment!!.requireActivity().supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                ?.addToBackStack(FragmentTag.MAP)
                ?.commit()
        }
    }


    /* Created by Axel Herbstreith
     * Stops the running simulation */
    fun stopSimulation() {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val courierId = CourierRepository.shared.getCourierId()!!
                                val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                                val body = ParcelJSONParser.createRequestEmptyBody()
                                val path = Path.SIMULATION_STOP

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        try {
                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_stop_simulation))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_stop_simulation))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_stop_simulation))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }
                        withContext(Dispatchers.Default) { bluetoothLeDeliveryService.endDelivery() }
                    } catch (e: Exception) {
                        Toast.createToast("Stopping delivery failed!")
                    }
                }
            }
        } else {
            //TODO: WHAT HAPPENS WHEN DEMO SCENARIO IS USED?
        }
    }


    /* This call updated the parcel order */
    fun updateParcelPosition(parcelId: String, newPos: Int) {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {

                            val courierId = CourierRepository.shared.getCourierId()!!
                            val header = ParcelJSONParser.createHeaderUpdateParcelPosition(uid, courierId)
                            val body = ParcelJSONParser.createRequestBodyUpdateParcelPosition(parcelId, newPos)

                            val path = Path.PARCEL_ORDER
                            apiController.put(path, header, body) { response ->
                                if (response != null) {
                                    val strResp = response.toString()
                                    try {
                                        val jsonObject = JSONObject(strResp)
                                        val parcelList =
                                            ParcelJSONParser.parseResponseToParcelListWithoutVerificationToken(jsonObject)

                                        ParcelRepository.shared.insertAll(parcelList)

                                    } catch (e: JSONException) {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_update_parcel_position))
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_update_parcel_position))
                                }
                            }
                        }
                    } else {
                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_update_parcel_position))
                    }
                }
        } else {
            val switchedParcel = ParcelRepository.shared.getParcelById(parcelId)
            val toBeSwitchedParcel = ParcelRepository.shared.getParcelByDeliveryPosition(newPos)

            val oldPos = switchedParcel.deliveryPosition

            switchedParcel.deliveryPosition = newPos
            toBeSwitchedParcel.deliveryPosition = oldPos

            ParcelRepository.shared.insert(switchedParcel)
            ParcelRepository.shared.insert(toBeSwitchedParcel)
        }
    }

    /* Sends next Parking Location to Server */
    fun postNextParkingLocation(paID: String) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {
                                val pA = ParkingAreaRepository.shared.getParkingAreaById(paID)
                                val courierId = CourierRepository.shared.getCourierId()!!
                                val path = Path.PARKING_NEXT
                                val params = ParkingAreaJSONParser.createHeaderGetAllParkingAreasRequest(uid, courierId)
                                val body = ParkingAreaJSONParser.createBodyPostNextParkingLocation(pA)
                                apiController.post(path, params, body) { response ->
                                    if (response != null) {

                                        val strResp = response.toString()
                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val parkingAreaResponseObject =
                                                ParkingAreaJSONParser.parseResponseToParkingAreaObjectSingle(jsonObject)

                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                                            e.printStackTrace()

                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                        }
                    }
            } else if(VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        val pA = ParkingAreaRepository.shared.getParkingAreaById(paID)
                        withContext(Dispatchers.IO) {
                            bluetoothLeDeliveryService.driveToPosition(
                                doubleArrayOf(
                                    pA.lat.toDouble(),
                                    pA.long_.toDouble()
                                ), 0.0f, 0.0f
                            )
                        }
                    } catch (e: Exception) {
                        Toast.createToast("Setting next parking location failed!")
                    }
                }
            }
    }

    fun getCurrentVanState() {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val path = Path.CURRENT_VAN_STATE
                                val params = VehicleJSONParser.createHeaderGetCurrentPosition(uid)
                                Log.i("VanAssistAPIController", "CALL GET CURRENT VAN STATE")
                                apiController.get(path, params) { response ->
                                    if (response != null) {

                                        val strResp = response.toString()
                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val currentStateResponseObject = VehicleJSONParser.parseResponseToState(response)
                                            Log.i("VanAssistAPIController", currentStateResponseObject.toString())

                                            val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)
                                            if (van == null) {
                                                VanRepository.shared.insert(
                                                    VanEntity(
                                                        VanAssistConfig.VAN_ID,
                                                        currentStateResponseObject.latitude,
                                                        currentStateResponseObject.longitude,
                                                        currentStateResponseObject.isParking,
                                                        currentStateResponseObject.doorStatus,
                                                        currentStateResponseObject.logisticStatus,
                                                        currentStateResponseObject.problemStatus,
                                                        currentStateResponseObject.problemMessage
                                                    )
                                                )
                                            } else {
                                                VanRepository.shared.updateVanById(
                                                    VanAssistConfig.VAN_ID,
                                                    currentStateResponseObject.latitude,
                                                    currentStateResponseObject.longitude,
                                                    currentStateResponseObject.isParking,
                                                    currentStateResponseObject.doorStatus,
                                                    currentStateResponseObject.logisticStatus,
                                                    currentStateResponseObject.problemStatus,
                                                    currentStateResponseObject.problemMessage
                                                )
                                                Log.i("VanAssistAPIController", "Cpos: " + van.latitude + " " + van.longitude)
                                            }
                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        launch() {
                            val vehicleID = async { bluetoothLeDeliveryService.getVehicleId() }
                            val vehicleStatus = async { bluetoothLeDeliveryService.getVehicleStatus() }

                            VanRepository.shared.updateVehicleStatusById(
                                vehicleID.await()!!,
                                VehicleJSONParser.parseVehicleStatusFromShort(vehicleStatus.await()[0])
                            )
                            VanRepository.shared.updateVanDoorStatusById(
                                vehicleID.await()!!,
                                VehicleJSONParser.parseVehicleDoorStatusFromVehicleStatus(vehicleStatus.await()[0])
                            )

                            val vehiclePosition = async(this.coroutineContext) { bluetoothLeDeliveryService.getVehiclePosition() }
                            VanRepository.shared.updateVanLocationById(
                                vehicleID.await()!!,
                                vehiclePosition.await()[0],
                                vehiclePosition.await()[1]
                            )
                        }
                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle status failed!")
                    }
                }
            }
        } else {
            if(VanRepository.shared.getVanById(VanAssistConfig.VAN_ID) == null) {
                VanRepository.shared.insert(
                    VanEntity(
                        VanAssistConfig.VAN_ID,
                        49.418075,
                        8.675032,
                        true,
                        "CLOSED",
                        "IN DELIVERY",
                        "OK",
                        ""
                    )
                )
            }
        }
    }

    fun sendDoorStatus(doorStatus: String) {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val courierId = CourierRepository.shared.getCourierId()!!
                                val header = VehicleJSONParser.createHeaderSetProblemSolved(uid)
                                val body = VehicleJSONParser.createBodySendDoorStatus(doorStatus)
                                val path = Path.SEND_DOOR_STATUS

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        try {
                                            this.getCurrentVanState()
                                        } catch (e: JSONException) {
                                            Toast.createToast("DOOR STATUS MESSAGE FAILED!")
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast("DOOR STATUS MESSAGE FAILED!")
                                    }
                                }
                            }
                        } else {
                            Toast.createToast("DOOR STATUS MESSAGE FAILED!")
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        if (doorStatus.equals("OPEN")) {
                            Log.i("APIController", "OPEN DOORS REQUEST")
                        }

                        //BLUETOOTH SERVICE. SEND VEHICLE STATUS (DOORS_OPEN OR PARKING)

                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle status failed!")
                    }
                }
            }
        } else {
            if(doorStatus.equals("CLOSED")) {
                VanRepository.shared.updateVanDoorStatusById(VanAssistConfig.VAN_ID, "CLOSED")
                VanRepository.shared.updateVehicleStatusById(VanAssistConfig.VAN_ID, "PARKING")
            } else {
                VanRepository.shared.updateVanDoorStatusById(VanAssistConfig.VAN_ID, "OPEN")
                VanRepository.shared.updateVehicleStatusById(VanAssistConfig.VAN_ID, "DOORSOPEN")
            }
        }
    }

    fun updateFCMToken() {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA && !VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                val fcmToken = instanceIdResult.token

                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val courierId = CourierRepository.shared.getCourierId()!!

                                val header = CourierJSONParser.createHeaderUpdateFCMToken(uid, courierId, fcmToken)
                                val body = ParcelJSONParser.createRequestEmptyBody()
                                val path = Path.UPDATE_FCM_TOKEN

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        try {
                                        } catch (e: JSONException) {
                                            Toast.createToast("Update of fcm token failed!")
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast("Update of fcm token failed!")
                                    }
                                }
                            }
                        } else {
                            Toast.createToast("Update of fcm token failed!")
                        }
                    }
            }
        }
    }

    fun getCurrentVanLocation() {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {

                            val path = Path.CURRENT_VAN_LOCATION
                            val params = VehicleJSONParser.createHeaderGetCurrentPosition(uid)
                            Log.i("VanAssistAPIController", "CALL GET CURRENT VAN LOCATION")
                            apiController.get(path, params) { response ->
                                if (response != null) {

                                    val strResp = response.toString()
                                    try {
                                        val jsonObject = JSONObject(strResp)
                                        val currentLocationResponseObject = VehicleJSONParser.parseResponseToLocation(jsonObject)
                                        Log.i("VanAssistAPIController", currentLocationResponseObject.toString())

                                        val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)
                                        if (van == null) {
                                            VanRepository.shared.insert(
                                                VanEntity(
                                                    VanAssistConfig.VAN_ID,
                                                    currentLocationResponseObject.latitude(),
                                                    currentLocationResponseObject.longitude(),
                                                    true,
                                                    "CLOSED",
                                                    "IN DELIVERY",
                                                    "AUTO_DRIVING",
                                                    ""
                                                )
                                            )
                                        } else {
                                            VanRepository.shared.updateVanLocationById(
                                                VanAssistConfig.VAN_ID,
                                                currentLocationResponseObject.latitude(),
                                                currentLocationResponseObject.longitude()
                                            )
                                            Log.i("VanAssistAPIController", "Cpos: " + van.latitude + " " + van.longitude)
                                        }
                                    } catch (e: JSONException) {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                                }
                            }
                        }
                    } else {
                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_current_van_location_loading))
                    }
                }
        }
        else {
            MainScope().launch(Dispatchers.IO) {
                try {
                    if(!bluetoothLeService.getServicesDiscovered()) {
                        checkConnectionStatus()
                    }

                    val vehiclePosition = async{bluetoothLeDeliveryService.getVehiclePosition()}
                    val vanID = async{bluetoothLeDeliveryService.getVehicleId()}

                    val van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)
                    if (van == null) {
                        val logisticStatus = async{bluetoothLeDeliveryService.getVehicleStatus()}

                        var doorStatus = ""
                        if (logisticStatus.await().get(0) == 5.toShort()) {
                            doorStatus = "OPEN"
                        }
                        else {
                            doorStatus = "CLOSED"
                        }
                        VanRepository.shared.insert(
                            VanEntity(
                                vanID.await()!!,
                                vehiclePosition.await()[0],
                                vehiclePosition.await()[1],
                                true,
                                doorStatus,
                                "IN DELIVERY",
                                VehicleJSONParser.parseVehicleStatusFromShort(logisticStatus.await()[0]),
                                ""
                            )
                        )
                    } else {
                        VanRepository.shared.updateVanLocationById(
                            VanAssistConfig.VAN_ID,
                            vehiclePosition.await()[0],
                            vehiclePosition.await()[1]
                        )
                        Log.i("VanAssistAPIController", "Cpos: " + van.latitude + " " + van.longitude)
                    }
                }catch (e: Exception) {
                    Toast.createToast("Getting vehicle position failed!")
                }
            }
        }
    }

    fun setProblemSolved(option: Int) {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {

                                val courierId = CourierRepository.shared.getCourierId()!!
                                val header = VehicleJSONParser.createHeaderSetProblemSolved(uid)
                                val body = VehicleJSONParser.createRequestEmptyBody()
                                val path = Path.PROBLEM_SOVLED

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        try {
                                            this.getCurrentVanState()
                                        } catch (e: JSONException) {
                                            Toast.createToast("PROBLEM SOLVED MESSAGE FAILED!")
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast("PROBLEM SOLVED MESSAGE FAILED!")
                                    }
                                }
                            }
                        } else {
                            Toast.createToast("PROBLEM SOLVED MESSAGE FAILED!")
                        }
                    }
            } else {
                //SEND OPEN_DOORS STATUS?
                //WHAT ARE THE DIFFERENT POSSIBILITIES TO SOLVE A PROBLEM (I THOUGHT CONTINUE MISSION AND OPEN_DOORS)
                //IM TESTSZENARIO IST DAS OPEN_DOORS
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        //SEND STATUS 4 (WAITING)
                        if (option == 0) {
                            bluetoothLeDeliveryService.setVehicleStatus(5)
                        } else {
                            bluetoothLeDeliveryService.setVehicleStatus(1)
                        }

                        //BLUETOOTH SERVICE. SEND VEHICLE STATUS (DOORS_OPEN OR PARKING)

                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle status failed!")
                    }
                }

            }
        } else {
            VanRepository.shared.updateVanProblemMessageById(VanAssistConfig.VAN_ID, "")
            VanRepository.shared.updateVehicleStatusById(VanAssistConfig.VAN_ID, "PARKING")
        }

    }

    /* Created by Axel Herbstreith and Jasmin Weimüller
 *
 * Sends the updated parcel status to the server, when request was successful, the updated parcel information will be send back to frontend
 * This updated parcel information are parsed and stored in the local db again, after this process, the parcel information in the parcel sheet (UI) will pe updated.
 *
 * @param
 * @return void
 **/
    fun confirmParcelDeliverySuccess(parcelId: String) {
        if(!VanAssistConfig.USE_DEMO_SCENARIO_DATA && !VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {
                                val courierId = CourierRepository.shared.getCourierId()!!
                                val path = Path.PARCEL_CONFIRM_DELIVERY_SUCCESS
                                val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                                val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcelId)

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        val strResp = response.toString()

                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                            ParcelRepository.shared.insert(parcel)
                                            FragmentRepo.mapFragmentOld?.setParcelInformation(main)
                                            if (FragmentRepo.parcelListFragmentDeliverySuccess != null) {
                                                FragmentRepo.parcelListFragmentDeliverySuccess!!.updateAdapter()
                                            }

                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        withContext(Dispatchers.Default) { bluetoothLeDeliveryService.writeDeliverySuccess(0) }

                        val parcel = ParcelRepository.shared.getParcelById(parcelId)
                        parcel.state = 1
                        ParcelRepository.shared.insert(parcel)

                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle position failed!")
                    }
                }
            }
        } else {
            val parcel = ParcelRepository.shared.getParcelById(parcelId)
            parcel.state = 1
            ParcelRepository.shared.insert(parcel)
        }
    }


    /* Confirms that the courier failed to deliver the parcel */
    fun confirmParcelDeliveryFailure(parcelId: String) {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {
                                val courierId = CourierRepository.shared.getCourierId()!!
                                val path = Path.PARCEL_CONFIRM_DELIVERY_FAILURE
                                val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                                val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcelId)

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        val strResp = response.toString()

                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                            ParcelRepository.shared.insert(parcel)
                                            FragmentRepo.mapFragmentOld?.setParcelInformation(main)
                                            if (FragmentRepo.parcelListFragmentDeliveryFailure != null) {
                                                FragmentRepo.parcelListFragmentDeliveryFailure!!.updateAdapter()
                                            }
                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                                    }

                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_parcel_delivery_confirmation))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        withContext(Dispatchers.Default) { bluetoothLeDeliveryService.writeDeliveryFailure(0) }

                        val parcel = ParcelRepository.shared.getParcelById(parcelId)
                        parcel.state = 2
                        ParcelRepository.shared.insert(parcel)

                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle position failed!")
                    }
                }
            }
        } else {
            val parcel = ParcelRepository.shared.getParcelById(parcelId)
            parcel.state = 2
            ParcelRepository.shared.insert(parcel)
        }
    }


    /* Undo of the parcel delivery confirmation */
    fun undoParcelDeliveryConfirmation(parcel: ParcelEntity) {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)!!
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = task.result!!.token
                            if (uid != null) {
                                val courierId = CourierRepository.shared.getCourierId()!!
                                val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                                val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcel.id)
                                val path = Path.PARCEL_DELIVERY_UNDO

                                apiController.put(path, header, body) { response ->
                                    if (response != null) {
                                        val strResp = response.toString()

                                        try {
                                            val jsonObject = JSONObject(strResp)
                                            val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                            ParcelRepository.shared.insert(parcel)
                                        } catch (e: JSONException) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_undo_parcel_delivery))
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_undo_parcel_delivery))
                                    }
                                }
                            }
                        } else {
                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_undo_parcel_delivery))
                        }
                    }
            } else {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        if (!bluetoothLeService.getServicesDiscovered()) {
                            checkConnectionStatus()
                        }

                        withContext(Dispatchers.Default) { bluetoothLeDeliveryService.writeUndoDeliveryStatus(0) }

                        val parcel = ParcelRepository.shared.getParcelById(parcel.id)
                        parcel.state = 0
                        ParcelRepository.shared.insert(parcel)

                    } catch (e: Exception) {
                        Toast.createToast("Getting vehicle position failed!")
                    }
                }
            }
        } else {
            val parcel = ParcelRepository.shared.getParcelById(parcel.id)
            parcel.state = 0
            ParcelRepository.shared.insert(parcel)
        }
    }

    fun sendTestProblem(message: String) {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && ! VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val uid = it.result!!.token
                        if (uid != null) {
                            val header = VehicleJSONParser.createHeaderSendTestProblem(uid)
                            val body = VehicleJSONParser.createBodySendTestProblem(message)
                            val path = Path.SEND_PROBLEM

                            apiController.put(path, header, body) {
                                if (it != null) {
                                    try {

                                    } catch (e: JSONException) {
                                        Toast.createToast("SEND TEST PROBLEM FAILED!")
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.createToast("SEND TEST PROBLEM FAILED!")
                                }
                            }
                        } else {
                            Toast.createToast("SEND TEST PROBLEM FAILED!")
                        }
                    }
                }
        }
        else {
            VanRepository.shared.updateVehicleStatusById(VanAssistConfig.VAN_ID, "HARDFAULT")
            VanRepository.shared.updateVanProblemMessageById(VanAssistConfig.VAN_ID, "The van has encountered a serious problem that requires your intervention! Move to the vehicle as quickly as possible!")
        }
    }

    /* Loads all parking areas from backend */
    fun getAllParkingLocations() {
        if (!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {

                            val courierId = CourierRepository.shared.getCourierId()!!
                            val path = Path.PARKING_ALL
                            val params = ParkingAreaJSONParser.createHeaderGetAllParkingAreasRequest(uid, courierId)

                            apiController.get(path, params) { response ->
                                if (response != null) {

                                    val strResp = response.toString()
                                    try {
                                        val jsonObject = JSONObject(strResp)
                                        val parkingAreaResponseObject =
                                            ParkingAreaJSONParser.parseResponseToParkingAreaObject(jsonObject)
                                        ParkingAreaRepository.shared.insertAll(parkingAreaResponseObject)
                                    } catch (e: JSONException) {
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                                }
                            }
                        }
                    } else {
                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                    }
                }
        } else {
            val pA1 = ParkingAreaEntity(
                    "pA1",
                    "H1",
                    0.0f,
                    "0",
                    "0",
                    0,
                    0.0f,
                    0.0f,
                    49.418075f,
                    8.675032f,
                    0.0f,
                    0.0f
                )
                ParkingAreaRepository.shared.insert(pA1)

                val pA2 = ParkingAreaEntity(
                    "pA2",
                    "H2",
                    0.0f,
                    "0",
                    "0",
                    0,
                    0.0f,
                    0.0f,
                    49.418608f,
                    8.675005f,
                    0.0f,
                    0.0f
                )
                ParkingAreaRepository.shared.insert(pA2)

                val pA3 = ParkingAreaEntity(
                    "pA3",
                    "H3",
                    0.0f,
                    "0",
                    "0",
                    0,
                    0.0f,
                    0.0f,
                    49.419364f,
                    8.674974f,
                    0.0f,
                    0.0f
                )
                ParkingAreaRepository.shared.insert(pA3)

            val pA4 = ParkingAreaEntity(
                "pA4",
                "H4",
                0.0f,
                "0",
                "0",
                0,
                0.0f,
                0.0f,
                49.419281f,
                8.676126f,
                0.0f,
                0.0f
            )
            ParkingAreaRepository.shared.insert(pA4)

            val pA5 = ParkingAreaEntity(
                "pA5",
                "H5",
                0.0f,
                "0",
                "0",
                0,
                0.0f,
                0.0f,
                49.417426f,
                8.676124f,
                0.0f,
                0.0f
            )
            ParkingAreaRepository.shared.insert(pA5)

            val pA6 = ParkingAreaEntity(
                "pA6",
                "H6",
                0.0f,
                "0",
                "0",
                0,
                0.0f,
                0.0f,
                49.419667f,
                8.672891f,
                0.0f,
                0.0f
            )
            ParkingAreaRepository.shared.insert(pA6)
        }
    }

    /* Sends next Parking Location to Server */
    fun requestParcelStateReset() {
        if(!VanAssistConfig.USE_BLUETOOTH_INTERFACE && !VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {
                            val courierId = CourierRepository.shared.getCourierId()!!
                            val path = Path.PARCEL_STATE_RESET
                            val params = ParkingAreaJSONParser.createHeaderGetAllParkingAreasRequest(uid, courierId)
                            apiController.get(path, params) { response ->
                                if (response != null) {

                                    val strResp = response.toString()
                                    try {
                                        Log.i("VanAssistAPIController", strResp)
                                        val json = JSONObject(strResp)
                                        if (json.getInt("status") == 200) {
                                            Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.parcel_status_reset_successful))
                                        }

                                    } catch (e: JSONException) {
                                        Log.i("VanAssistAPIController", "Invalid string response received")
                                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_request_parcel_state_reset))
                                        e.printStackTrace()

                                    }
                                } else {
                                    Log.i("VanAssistAPIController", "Response is null!")
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_request_parcel_state_reset))
                                }
                            }
                        }
                    } else {
                        Log.i("VanAssistAPIController", "Task was not successfull!")
                        Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_request_parcel_state_reset))
                    }
                }
        } else {
            val pList = ParcelRepository.shared.getAll()

            for(parcel in pList) {
                parcel.state = 0
                ParcelRepository.shared.insert(parcel)
            }
        }
    }

    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier darkmode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableDarkMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)
                                    main.delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                    //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                    main.recreate()

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_activation))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_activation))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_activation))
                }
            }
    }


    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier darkmode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableDarkMode() {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)
                                    main.delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                    main.recreate()

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_deactivation))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_deactivation))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_dark_mode_deactivation))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier intelligence mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableAmbientIntelligenceMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_AMBIENT_INTELLIGENCE_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier intelligence mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableAmbientIntelligenceMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_AMBIENT_INTELLIGENCE_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_deactivation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.ambient_intelligence_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier time based dark mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableTimeBasedDarkMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_TIME_BASED_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier time based dark mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableTimeBasedDarkMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_TIME_BASED_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_deactivation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.time_dependent_darkmode_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier size dependent waiting mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableSizeDependentWaitingMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_SIZE_DEPENDENT_WAITING_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier size dependent waiting mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableSizeDependentWaitingMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_SIZE_DEPENDENT_WAITING_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    //Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                //Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_deactivation_failed))
                            }
                        }
                    }
                } else {
                    //Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.size_dependent_waiting_mode_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier dynamic content mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableDynamicContentMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_DYNAMIC_CONTENT_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier dynamic content mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableDynamicContentMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_DYNAMIC_CONTENT_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_deactivation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.dynamic_content_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier gamification mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableGamificationMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_GAMIFICATION_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier gamification mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableGamificationMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_GAMIFICATION_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_deactivation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.gamification_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier intelligent driving mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun enableIntelligentDrivingMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_INTELLIGENT_DRIVING_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_activation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_activation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_activation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier intelligent driving mode setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     **/
    fun disableIntelligentDrivingMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = CourierJSONParser.createDefaultHeader(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_INTELLIGENT_DRIVING_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_deactivation_failed))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_deactivation_failed))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.intelligent_driving_deactivation_failed))
                }
            }
    }


    /* Created by Axel Herbstreith
     *
     * Sends the updated courier language setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param languageCode
     * @return void
     **/
    fun changeLanguage(languageCode: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = CourierJSONParser.createRequestBodyChangeLanguage(languageCode)
                        val path = Path.CHANGE_LANGUAGE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val newCourier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(newCourier)

                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_change_language))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_change_language))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_change_language))
                }
            }
    }


    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier map label setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     */
    fun enableHelpMode() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_MAP_LABEL

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_activation))
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_activation))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_activation))
                }
            }
    }


    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier map label setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     */
    fun disableHelpMode() {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        val courierId = CourierRepository.shared.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_MAP_LABEL

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()
                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    CourierRepository.shared.insert(courier)
                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_deactivation))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_deactivation))
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_help_label_deactivation))
                }
            }
    }

}