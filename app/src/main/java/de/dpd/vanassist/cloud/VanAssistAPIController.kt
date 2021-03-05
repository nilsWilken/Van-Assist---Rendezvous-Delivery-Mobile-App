package de.dpd.vanassist.cloud

import android.app.ProgressDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import org.json.JSONException
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth
import de.dpd.vanassist.R
import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.config.Path
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.fragment.main.MapFragmentOld
import de.dpd.vanassist.util.*
import de.dpd.vanassist.util.date.DateParser
import de.dpd.vanassist.util.json.CourierJSONParser
import de.dpd.vanassist.util.json.ParcelJSONParser
import de.dpd.vanassist.util.json.ParkingAreaJSONParser
import com.google.firebase.iid.FirebaseInstanceId
import com.mapbox.geojson.Point
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.util.json.VehicleJSONParser
import de.dpd.vanassist.util.toast.Toast
import okhttp3.*
import java.io.IOException


class VanAssistAPIController(activity: AppCompatActivity, context: Context) {

    private val service = ServiceVolley()
    private val apiController = APIController(service)
    val main = activity

    private val bluetoothLeService = BluetoothLeServiceImpl.getInstance(context)
    private val bluetoothLeDeliveryService = BluetoothLeDeliveryService.getInstance(bluetoothLeService)

    private lateinit var auth: FirebaseAuth


    /* Created by Axel Herbstreith and Jasmin Weimüller
     * Starts the log in process for the courier
     * Firebase Authentication is handled in this method, if successful courier information will be loaded from server
     *
     * @param activity, username, password
     * @return void */
    fun userAuthentication(act: AppCompatActivity, userName: String, password: String) {
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
    private fun loadAndSaveCourierInformation(act: AppCompatActivity, dialog: ProgressDialog, userName: String) {
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
                                        dialog.dismiss()
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


    /* Created by Axel Herbstreith and Jasmin Weimüller
     *
     * Requests all parcel information from server, parses the response and stores it on the local database
     *
     * @param
     * @return void
     **/
    fun loadAndSaveAllParcel() {
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
    }


    /* Confirms that the courier failed to deliver the parcel */
    fun confirmParcelDeliveryFailure(parcelId: String) {
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
    }


    /* Undo of the parcel delivery confirmation */
    fun undoParcelDeliveryConfirmation(parcel: ParcelEntity) {

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

    fun updateFCMToken() {
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


    /* Starts the simulation
     * This method needs to be executed by another framework than volley (in our case OkHttp3) since there was an issue with using Volley -> see report) */
    fun startSimulation() {

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
    }


    /* Created by Axel Herbstreith
     * Stops the running simulation */
    fun stopSimulation() {

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
    }


    /* This call updated the parcel order */
    fun updateParcelPosition(parcelId: String, newPos: Int) {
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
                                    val parcelList = ParcelJSONParser.parseResponseToParcelListWithoutVerificationToken(jsonObject)

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
    }


    /* Loads all parking areas from backend */
    fun getAllParkingLocations() {
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
                                    val parkingAreaResponseObject = ParkingAreaJSONParser.parseResponseToParkingAreaObject(jsonObject)
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
    }


    fun getCurrentVanLocation() {
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
                                    if(van == null) {
                                        VanRepository.shared.insert(
                                            VanEntity(
                                                VanAssistConfig.VAN_ID,
                                                currentLocationResponseObject.latitude(),
                                                currentLocationResponseObject.longitude(),
                                                true,
                                                "CLOSED",
                                                "IN DELIVERY",
                                                "OK",
                                                ""
                                            )
                                        )
                                    }
                                    else {
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


    fun getCurrentVanState() {
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
                                    if(van == null) {
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
                                    }
                                    else {
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

                                    var frag = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag(FragmentTag.VEHICLE_STATUS)
                                    if(frag != null && frag.isVisible) {
                                        //frag.onResume()
                                        val transaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
                                        transaction?.detach(frag)
                                        transaction?.attach(frag)
                                        transaction?.commit()
                                    }

                                    frag = FragmentRepo.mapActivity?.supportFragmentManager?.findFragmentByTag(FragmentTag.VEHICLE_PROBLEM_DETAILS)
                                    if(frag != null && frag.isVisible) {
                                        //frag.onResume()
                                        val transaction = FragmentRepo.mapActivity?.supportFragmentManager?.beginTransaction()
                                        transaction?.detach(frag)
                                        transaction?.attach(frag)
                                        transaction?.commit()
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

    fun setProblemSolved() {
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

    }

    fun sendDoorStatus(doorStatus: String) {
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

    }

    fun sendTestProblem(message: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)!!
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    val uid = it.result!!.token
                    if(uid != null) {
                        val header = VehicleJSONParser.createHeaderSendTestProblem(uid)
                        val body = VehicleJSONParser.createBodySendTestProblem(message)
                        val path = Path.SEND_PROBLEM

                        apiController.put(path, header, body) {
                            if(it != null) {
                                try{

                                }catch(e: JSONException) {
                                    Toast.createToast("SEND TEST PROBLEM FAILED!")
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.createToast("SEND TEST PROBLEM FAILED!")
                            }
                        }
                    }else {
                        Toast.createToast("SEND TEST PROBLEM FAILED!")
                    }
                }
            }
    }


    /* Sends next Parking Location to Server */
    fun postNextParkingLocation(paID: String) {
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
    }

    /* Sends next Parking Location to Server */
    fun requestParcelStateReset() {
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
                                    if(json.getInt("status") == 200) {
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
    }
}