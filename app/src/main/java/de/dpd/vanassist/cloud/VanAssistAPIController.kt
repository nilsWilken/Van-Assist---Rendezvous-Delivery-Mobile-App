package de.dpd.vanassist.cloud

import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import com.google.android.gms.tasks.OnCompleteListener
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.config.Path
import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.util.Vehicle
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.fragment.main.MapFragment
import de.dpd.vanassist.util.*
import de.dpd.vanassist.util.date.DateParser
import de.dpd.vanassist.util.json.CourierJSONParser
import de.dpd.vanassist.util.json.ParcelJSONParser
import de.dpd.vanassist.util.json.ParkingAreaJSONParser
import com.google.firebase.iid.FirebaseInstanceId
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.util.toast.Toast
import okhttp3.*
import java.io.IOException
import kotlin.collections.HashMap




class VanAssistAPIController(act: AppCompatActivity) {

    private val service = ServiceVolley()
    var resultAPI = HashMap<String, String?>()
    private val apiController = APIController(service)
    val main = act
    lateinit var parcelRepo: ParcelRepository
    lateinit var courierRepo: CourierRepository
    lateinit var  parkingAreaRepo: ParkingAreaRepository



    private lateinit var auth: FirebaseAuth


    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Starts the log in process for the courier
     * Firebase Authentication is handled in this method, if successful courier information will be loaded from server
     *
     * @param activity, username, password
     * @return void
     **/
    fun userAuthentication(act: AppCompatActivity, userName: String, password: String) {
        courierRepo = CourierRepository(main)
        val dialog = ProgressDialog.show(act, "", act.getString(R.string.authenticating___), true)
        auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(userName, password)
            .addOnCompleteListener(act) { task ->
                if (task.isSuccessful) {

                    parcelRepo = ParcelRepository(main)
                    parkingAreaRepo = ParkingAreaRepository(main)
                    loadAndSaveCourierInformation(act, dialog, userName)
                    //getAllParkingLocations()
                } else {
                    dialog.dismiss()
                    android.widget.Toast.makeText(act.baseContext, act.getString(R.string.authentication_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Load and saves the courier information
     * --> Get the Firebase Token
     * --> Create params for api call
     * Then all parcel information are loaded in background
     * Then, the map Activity is only started when
     *  - the data was retrieved from server, stored on the local database and checked if exactly one user entry is in the local DB
     *  - All permissions are granted, if not --> start Permission Activity
     *
     * @param activity, dialog
     * @return Void
     **/
    private fun loadAndSaveCourierInformation(act: AppCompatActivity, dialog: ProgressDialog, userName:String) {
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
                                val strResp = response.toString()
                                try {
                                    val jsonObject =  JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    Log.wtf("courier info","->" + courier.languageCode)
                                    courierRepo.insert(courier)

                                    if (courierRepo.getAll().count() == 1) {
                                        loadAndSaveAllParcel()
                                        dialog.dismiss()
                                        MapActivity.start(act)
//                                        if(PermissionHandler.permissionGranted(act))  {
//                                            MapActivity.start(act)
//                                        } else {
//                                            PermissionActivity.start(act)
//                                        }
                                    }

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            } else {
                                Log.d("Error", "catch load and save courier")
                            }
                        }
                    }
                } else {
                }
            }
    }


    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Requests all parcel information from server, parses the response and stores it on the local database
     *
     * @param
     * @return void
     **/
     fun loadAndSaveAllParcel() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        parcelRepo = ParcelRepository(main)
                        courierRepo = CourierRepository(main)
                        parkingAreaRepo = ParkingAreaRepository(main)
                        val courier = courierRepo.getCourier()!!
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
                                    courierRepo.updateVerificationToken(parcelResponseObject.verificationToken)
                                    parcelRepo.insertAll(parcelResponseObject.parcelList)
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
    }

    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated parcel status to the server, when request was successful, the updated parcel information will be send back to frontend
     * This updated parcel information are parsed and stored in the local db again, after this process, the parcel information in the parcel sheet (UI) will pe updated.
     *
     * @param
     * @return void
     **/
    fun confirmParcelDeliverySuccess(parcelId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val path = Path.PARCEL_CONFIRM_DELIVERY_SUCCESS
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcelId)

                        apiController.put(path, header, body) { response ->
                            Log.wtf("response", response.toString())
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                    parcelRepo = ParcelRepository(main)
                                    parcelRepo.insert(parcel)
                                    FragmentRepo.mapFragment?.setParcelInformation()
                                    if(FragmentRepo.parcelListFragmentDeliverySuccess != null) {
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
    }




    /*
     *
     * Parcel ... Failure Parcel Order
     *
     */
    fun confirmParcelDeliveryFailure(parcelId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val path = Path.PARCEL_CONFIRM_DELIVERY_FAILURE
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcelId)

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                    parcelRepo = ParcelRepository(main)
                                    parcelRepo.insert(parcel)
                                    FragmentRepo.mapFragment?.setParcelInformation()
                                    if(FragmentRepo.parcelListFragmentDeliveryFailure != null) {
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
    }


    /*
    *
    * Parcel ... Undo Parcel Delivery Confirmation
    *
    */
    fun undoParcelDeliveryConfirmation(parcel: Parcel) {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createBodyConfirmParcelRequest(parcel.id)
                        val path = Path.PARCEL_DELIVERY_UNDO

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val parcel = ParcelJSONParser.parseDeliveryConfirm(jsonObject)
                                    parcelRepo = ParcelRepository(main)
                                    parcelRepo.insert(parcel)
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
    }


    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
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

                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    courierRepo.insert(courier)
                                    var test = courierRepo.getCourier()
                                    Log.d("API COURIER THEME", test!!.darkMode.toString())
                                    main!!.recreate()


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



    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
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

                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_DARK_MODE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    courierRepo.insert(courier)
                                    var test = courierRepo.getCourier()
                                    Log.d("API COURIER THEME", test!!.darkMode.toString())
                                    main!!.recreate()

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


    /**
     * Created by Axel Herbstreith
     *
     * Sends the updated courier language setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param languageCode
     * @return void
     **/
    fun changeLanguage(languageCode:String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = CourierJSONParser.createRequestBodyChangeLanguage(languageCode)
                        val path = Path.CHANGE_LANGUAGE

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val newCourier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    courierRepo.insert(newCourier)

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




    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier map label setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     */
    fun enableMapLabel() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {

                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.ENABLE_MAP_LABEL

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()

                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    courierRepo.insert(courier)
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


    /**
     * Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Sends the updated courier map label setting to the server, when request was successful, the updated courier information will be send back to frontend
     * This updated courier information are parsed and stored in the local db again
     *
     * @param
     * @return void
     */
    fun disableMapLabel() {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.DISABLE_MAP_LABEL

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()
                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val courier = CourierJSONParser.parseResponseToCourierObject(jsonObject)
                                    courierRepo.insert(courier)
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


    fun startSimulation() {

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val fcmToken = instanceIdResult.token

            val user = FirebaseAuth.getInstance().currentUser
            user?.getIdToken(true)!!
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result!!.token
                        if (uid != null) {
                            courierRepo = CourierRepository(main)
                            val courierId = courierRepo.getCourierId()!!
                            val url = Path.BASE_PATH + Path.SIMULATION_START
                            val client = OkHttpClient()
                            val secondsSinceMidnight = DateParser.getSecondsSinceMidnight().toString()

                            val head = mutableMapOf<String, String>()
                            head.put("uid", uid)
                            head.put("courier_id", courierId)
                            head.put("seconds_since_midnight", secondsSinceMidnight)
                            head.put("fcm_token", fcmToken)
                            val headerbuild = Headers.of(head)
                            //val params = CourierJSONParser.createHeaderStartSimulation(uid, courierId, secondsSinceMidnight.toLong())

                            val request = Request.Builder().url(url).headers(headerbuild).build()



                            client.newCall(request).enqueue(object: Callback {
                                override fun onFailure(call:Call, e:IOException) {
                                    e.printStackTrace();
                                }

                                @Throws(IOException::class)
                                override fun onResponse(call:Call, response:Response) {
                                    if (response.isSuccessful) {
                                        if (response != null) {
                                            val strResp = response.body()!!.string()
                                            try {
                                                val jsonObject = JSONObject(strResp)
                                                val data = jsonObject.getJSONObject("data")
                                                val simulationIsRunning = data.getBoolean("simulation_is_running")

                                                if(simulationIsRunning) {

                                                    FragmentRepo.launchPadFragment!!.dialog!!.dismiss()
                                                    val mapFragment = MapFragment.newInstance()
                                                    FragmentRepo.launchPadFragment!!.activity!!.supportFragmentManager
                                                        ?.beginTransaction()
                                                        ?.replace(R.id.map_activity, mapFragment, FragmentTag.MAP)
                                                        ?.addToBackStack(FragmentTag.MAP)
                                                        ?.commit()
                                                }

                                            } catch (e: JSONException) {
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
    }




    /**
     * Created by Axel Herbstreith
     *
     * @param
     * @return void
     */
    fun stopSimulation() {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderConfirmDeliveryRequest(uid, courierId)
                        val body = ParcelJSONParser.createRequestEmptyBody()
                        val path = Path.SIMULATION_STOP

                        apiController.put(path, header, body) { response ->
                            if (response != null) {
                                val strResp = response.toString()
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
    }




    /*
     *
     * Parcel ... Update Parcel Order
     *
     */
    fun updateParcelPosition(parcelId:String, newPos:Int) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        courierRepo = CourierRepository(main)
                        parcelRepo = ParcelRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val header = ParcelJSONParser.createHeaderUpdateParcelPosition(uid, courierId)
                        val body = ParcelJSONParser.createRequestBodyUpdateParcelPosition(parcelId, newPos)

                        val path = Path.PARCEL_ORDER
                        apiController.put(path, header , body) { response ->
                            if (response != null) {
                                val strResp = response.toString()
                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val parcelList = ParcelJSONParser.parseResponseToParcelListWithoutVerificationToken(jsonObject)
                                    parcelRepo.insertAll(parcelList)
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
    }

/*
 *
 * Vehicle ... Get All Parking Locations
 *
 */

    fun getAllParkingLocations() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        val courierRepo = CourierRepository(main)
                        val courierId = courierRepo.getCourierId()!!
                        val path = Path.PARKING_ALL
                        val params = ParkingAreaJSONParser.createHeaderGetAllParkingAreasRequest(uid,courierId)

                        apiController.get(path, params) { response ->
                            if (response != null) {

                                val strResp = response.toString()
                                try {
                                    parkingAreaRepo = ParkingAreaRepository(main)
                                    val jsonObject = JSONObject(strResp)
                                    val parkingAreaResponseObject = ParkingAreaJSONParser.parseResponseToParkingAreaObject(jsonObject)
                                    Log.d("PARKING",parkingAreaResponseObject.toString())
                                    parkingAreaRepo.insertAll(parkingAreaResponseObject)
                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                                Log.d("Error", response.toString())
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_load_parking_location))
                }
            }
    }

    /*
 *
 * Vehicle ... Get All Parking Locations
 *
 */

    fun postNextParkingLocation(paID: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result!!.token
                    if (uid != null) {
                        val courierRepo = CourierRepository(main)
                        val parkingAreaRepo = ParkingAreaRepository(main)
                        val pA = parkingAreaRepo.getParcelById(paID)
                        val courierId = courierRepo.getCourierId()!!
                        val path = Path.PARKING_NEXT
                        val params = ParkingAreaJSONParser.createHeaderGetAllParkingAreasRequest(uid,courierId)
                        val body = ParkingAreaJSONParser.createBodyPostNextParkingLocation(pA)
                        apiController.post(path, params, body) { response ->
                            if (response != null) {

                                val strResp = response.toString()
                                try {
                                    val jsonObject = JSONObject(strResp)
                                    val parkingAreaResponseObject = ParkingAreaJSONParser.parseResponseToParkingAreaObjectSingle(jsonObject)
                                    Log.d("SINGLE PARKING",parkingAreaResponseObject.toString())
                                    //parkingAreaRepo.insertAll(parkingAreaResponseObject)

                                    //Do something with this object
                                } catch (e: JSONException) {
                                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                                    e.printStackTrace()

                                }
                            } else {
                                Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                                Log.d("Error", "Error with Parking Area Request")
                                Log.d("Error", response.toString())
                            }
                        }
                    }
                } else {
                    Toast.createToast(FragmentRepo.mapActivity!!.getString(R.string.error_set_next_parking_location))
                }
            }
    }



 /*********************************************NOT_IMPLEMENTED**************************************************/


    fun getCurrentUid() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        Log.d("UID", local_idToken)
                    }
                } else {
                }
            })
    }

    /*
     *
     * Vehicle ... Get Current Location of Vehicle
     *
     */
    fun getLocation() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    // DO EVERYTHING IN HERE
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        Log.d("UID", local_idToken)
                        Log.d("UID", local_idToken.length.toString())
                        var params = JSONObject()
                        params.put("uid", local_idToken)
                        /* FOR OVERRIDING the usual headers
                        params.put("courier_id", "vanassist")
                        params.put("van_id", "")
                        */
                        var lat = "";
                        var long = "";
                        var message: String
                        var status: String
                        var path = "vehicle/location"

                        apiController.get(path, params) { response ->
                            if (response != null) {
                                var strResp = response.toString()
                                Log.d("Location", strResp)
                                try {

                                    var jsonObject = JSONObject(strResp)
                                    var jo = jsonObject.getJSONObject("data")
                                    lat = jo.getString("latitude")
                                    long = jo.getString("longitude")
                                    message = jsonObject.getString("message").toString()
                                    status = jsonObject.getString("status").toString()

                                    var loc_vehicle = Vehicle(lat, long)

                                    /*
                                    NOW WORK WITH VEHICLE
                                     */
                                    Log.d("RESULT_API", resultAPI.size.toString())
                                    var txtView = main.findViewById(de.dpd.vanassist.R.id.txtResult) as TextView
                                    txtView.text = strResp

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                                Log.d("RESULT_API", resultAPI.toString())
                                var statusResult = "";
                                statusResult = resultAPI.get("status").toString()
                                Log.d("RESULT_API", statusResult)
                                /*
                                EVERYTHING WE WANT TO DO WITH THE RESULT SHOULD GO IN HERE!
                                 */
                            } else {
                                Log.d("Error", "catch get Location")
                            }
                        }
                    }
                } else {
                }
            })
    }


    /*
     *
     * Vehicle ... Summon Van
     *
     */

    fun putSummonVan(loc_courier_lat: String, loc_courier_long: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    // DO EVERYTHING IN HERE
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        Log.d("UID", local_idToken)
                        Log.d("UID", local_idToken.length.toString())
                        var params = JSONObject()
                        params.put("uid", local_idToken)
                        /* FOR OVERRIDING the usual headers
                        params.put("courier_id", "vanassist")
                        params.put("van_id", "")
                        */
                        var innerparams = JSONObject()
                        var path = "vehicle/location"

                        var message: String
                        var status: String
                        var lat = loc_courier_lat;
                        var long = loc_courier_long;

                        innerparams.put("latitude", lat)
                        innerparams.put("longitude", long)
                        params.put("location", innerparams)

                        apiController.get(path, params) { response ->
                            if (response != null) {
                                //do Response Parsing
                                var strResp = response.toString()
                                Log.d("Summon var", strResp)
                                try {
                                    var jsonObject = JSONObject(strResp)
                                    var jo = jsonObject.getJSONObject("data")
                                    lat = jo.getString("latitude")
                                    long = jo.getString("longitude")
                                    message = jsonObject.getString("message").toString()
                                    status = jsonObject.getString("status").toString()

                                    var loc_vehicle = Vehicle(lat, long)
                                    var txtView = main.findViewById(de.dpd.vanassist.R.id.txtResult) as TextView
                                    txtView.text = strResp
                                    /*
                                    NOW WORK WITH VEHICLE
                                     */

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                                /*
                                EVERYTHING WE WANT TO DO WITH THE RESULT SHOULD GO IN HERE!
                                 */
                            } else {
                                Log.d("Error", "catch summon van")
                            }
                        }
                    }
                } else {
                }
            })
    }


/*
 *
 * Vehicle ... get UID
 *
 */


    fun getUID() {

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    // DO EVERYTHING IN HERE
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        //  idToken = local_idToken
                        Log.d("UID", local_idToken)
                        Log.d("UID", local_idToken.length.toString())

                        var params = JSONObject()
                        params.put("uid", local_idToken)
                        /* FOR OVERRIDING the usual headers
                        params.put("courier_id", "vanassist")
                        params.put("van_id", "")
                        */

                        var path = "uid"
                        var message: String
                        var status: String

                        apiController.get(path, params) { response ->
                            if (response != null) {
                                //do Response Parsing
                                try {

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                                /*
                                EVERYTHING WE WANT TO DO WITH THE RESULT SHOULD GO IN HERE!
                                 */
                            } else {
                                Log.d("Error", "catch get UID")
                            }
                        }
                    }
                } else {
                }
            })
    }






    /*
*
* Courier ... Get Courier Information
*
*/

    /*
*
* Courier ... Start Delivery Day
*
*/
    fun postStartDeliveryDay() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    // DO EVERYTHING IN HERE
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        Log.d("UID", local_idToken)
                        Log.d("UID", local_idToken.length.toString())

                        var params = JSONObject()
                        params.put("uid", local_idToken)
                        /* FOR OVERRIDING the usual headers
                        params.put("courier_id", "vanassist")
                        params.put("van_id", "")
                        */

                        var path = "courier/day/start" //"vehicle/location"
                        var message: String
                        var status: String

                        apiController.post(path, params, JSONObject()) { response ->
                            if (response != null) {
                                var strResp = response.toString()
                                Log.d("All parcels", strResp)

                                try {
                                    var jsonObject = JSONObject(strResp)
                                    var jo = jsonObject.getJSONObject("data")
                                    message = jsonObject.getString("message").toString()
                                    status = jsonObject.getString("status").toString()
                                    var txtView = main.findViewById(de.dpd.vanassist.R.id.txtResult) as TextView
                                    txtView.text = strResp

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                                /*
                                EVERYTHING WE WANT TO DO WITH THE RESULT SHOULD GO IN HERE!
                                 */
                            } else {
                                Log.d("Error", "catch post start day")
                            }
                        }
                    }
                } else {
                }
            })
    }

    /*
    *
    * Courier ... Finish Delivery Day
    *
    */
    fun putFinishDeliveryDay() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener(OnCompleteListener<GetTokenResult> { task ->
                if (task.isSuccessful) {
                    // DO EVERYTHING IN HERE
                    val local_idToken = task.result!!.token
                    if (local_idToken != null) {
                        Log.d("UID", local_idToken)
                        Log.d("UID", local_idToken.length.toString())

                        var params = JSONObject()
                        params.put("uid", local_idToken)
                        /* FOR OVERRIDING the usual headers
                        params.put("courier_id", "vanassist")
                        params.put("van_id", "")
                        */

                        var path = "courier/day/finish" //"vehicle/location"
                        var message: String
                        var status: String

                        apiController.put(path, params, JSONObject()) { response ->
                            if (response != null) {
                                var strResp = response.toString()
                                Log.d("All parcels", strResp)

                                try {
                                    var jsonObject = JSONObject(strResp)
                                    var jo = jsonObject.getJSONObject("data")
                                    message = jsonObject.getString("message").toString()
                                    status = jsonObject.getString("status").toString()
                                    var txtView = main.findViewById(de.dpd.vanassist.R.id.txtResult) as TextView
                                    txtView.text = strResp
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                                /*
                                EVERYTHING WE WANT TO DO WITH THE RESULT SHOULD GO IN HERE!
                                */
                            } else {
                                Log.d("Error", "catch finish delivery day")
                            }
                        }
                    }
                } else {
                }
            })
    }

}