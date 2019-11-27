package de.dpd.vanassist.fragment.main

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepmap.ebdemoapp.view.LevelSwitch
import com.deepmap.ebdemoapp.view.LevelSwitchListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hdm_i.dm.android.location.LocationTrackingMode
import com.hdm_i.dm.android.location.provider.GPSLocationProvider
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.MapBoxConfig
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.controls.SwipeButton
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.entity.ParkingAreaEntity
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.gps.GPSService
import de.dpd.vanassist.gps.Position
import de.dpd.vanassist.intelligence.dynamicContent.DynamicContent
import de.dpd.vanassist.intelligence.gamification.GamificationMode
import de.dpd.vanassist.intelligence.intelligentDriving.IntelligentDriving
import de.dpd.vanassist.intelligence.sizeDependentWaiting.SizeDependentWaiting
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.parcel.ParcelUtil
import de.dpd.vanassist.util.parkingArea.ParkingAreaUtil
import de.dpd.vanassist.util.toast.Toast
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_map_old.*
import kotlinx.android.synthetic.main.fragment_map_old.view.*
import java.util.*


var requestSend = false

/* A simple [Fragment] subclass. */
@Suppress("DEPRECATION")
class MapFragmentOld : Fragment() {

    /* ParcelCard Interaction */
    private var bottomSheetLinearLayout: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var bottomSheetStreetName: TextView? = null
    private var bottomSheetStreetNameAdditionalInformation: TextView? = null
    private var bottomSheetRecipientName: TextView? = null
    private var bottomSheetRecipientNameAdditionalInformation: TextView? = null
    private var bottomSheetPhoneButton: Button? = null
    private var currentParcel: ParcelEntity? = null
    var dialog: ProgressDialog? = null

    /* Capturing original camera position to reset */

    /* parkingArea Interaction */
    private lateinit var parkingAreas: List<ParkingAreaEntity>
    var nextParkingArea: ParkingAreaEntity? = null
    private var markerSelected = false

    /* Animations for the floating buttons */
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation
    private lateinit var fabRotateClockwise: Animation
    private lateinit var fabRotateAnticlockwise: Animation
    private var isOpen = false

    private var broadcastReceiver: BroadcastReceiver? = null

    /* LocationService */
    companion object {
        var gpsService: Intent? = null
        fun newInstance(): MapFragmentOld {
            FragmentRepo.mapFragmentOld = MapFragmentOld()
            return FragmentRepo.mapFragmentOld as MapFragmentOld
        }
    }

    private var permissions =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var wasclicked = false
    private var wasClickedDeliveryLocation = false
    private var wasClickedVanLocation = false
    private var routeShown = false
    private var deliveryRouteShown = false
    private var vehicleLocation = MapBoxConfig.DEFAULT_VEHICLE_LOCATION
    private var is3D = false

    /* Converts VectorDrawable to Bitmap */
    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {

        val drawable = ContextCompat.getDrawable(context, drawableId)

        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.locationProvider = GPSLocationProvider.Builder(context).create()
        mapView.setReadyDelegate {
            mapView.level = 0
            mapView.setUserTrackingMode(LocationTrackingMode.FOLLOW)

            levelSwitch.layoutManager = LinearLayoutManager(context)
            val adapter = LevelSwitch(mapView.maximumLevel)
            adapter.levelSwitchListener = object : LevelSwitchListener {
                override fun onLevelClick(level: Int) {
                    mapView.level = level
                }
            }
            levelSwitch.adapter = adapter
            set3DButton()

            mapView.showCompass(false)
            mapView.set3DMode(is3D, false)
            button3D.setOnClickListener {
                is3D = !is3D
                set3DButton()
                mapView.set3DMode(is3D, true)
            }
        }
    }

    private fun set3DButton() {
        val ctx = context ?: return
        if (is3D) {
            button3DText.setTextColor(ContextCompat.getColor(ctx, R.color.nearly_black))
        } else {
            button3DText.setTextColor(ContextCompat.getColor(ctx, R.color.light_black))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_map_old, container, false)

        /* Set Default Van Location only first time after simulation start */
        /* /> not when simulation is running and it is only resumed */
        /* /> would overwrite last position */
        if (SimulationConfig.isFirstVanLocationAfterSimulationStart) {
            VanRepository.shared.insert(
                VanEntity(
                    VanAssistConfig.VAN_ID,
                    0.0, 0.0,
                    true
                )
            )
            SimulationConfig.isFirstVanLocationAfterSimulationStart = false
        }

        if (testTargetApi() || checkPermissions()) {
            this.currentParcel = ParcelRepository.shared.getCurrentParcel()

            val api = VanAssistAPIController(activity!! as AppCompatActivity)

            /* declaring the animations */
            fabOpen = AnimationUtils.loadAnimation(context, R.anim.fab_open)
            fabClose = AnimationUtils.loadAnimation(context, R.anim.fab_close)
            fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            fabRotateClockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_clockwise)
            fabRotateAnticlockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_anticlockwise)


            /* Config of bottom sheet aka parcel card */
            bottomSheetStreetName = v.bottom_sheet_street_text_view as TextView
            bottomSheetStreetNameAdditionalInformation =
                v.bottom_sheet_street_additional_information_text_view as TextView
            bottomSheetRecipientName = v.bottom_sheet_recipient_name_text_view as TextView
            bottomSheetRecipientNameAdditionalInformation =
                v.bottom_sheet_recipient_name__additional_information_text_view as TextView
            bottomSheetPhoneButton = v.bottom_sheet_phone_button as Button

            val courier = CourierRepository.shared.getCourier()

            if (courier?.darkMode!!) {
                bottomSheetStreetName!!.setTextColor(Color.WHITE)
                bottomSheetRecipientName!!.setTextColor(Color.WHITE)
            } else {
                bottomSheetStreetName!!.setTextColor(Color.BLACK)
                bottomSheetRecipientName!!.setTextColor(Color.BLACK)
            }

            bottomSheetPhoneButton!!.setOnClickListener {
                if (currentParcel?.phoneNumber != "" && currentParcel?.phoneNumber != null) {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:" + currentParcel?.phoneNumber)
                        startActivity(callIntent)
                    } catch (activityException: ActivityNotFoundException) {
                    }
                } else {
                    bottomSheetPhoneButton?.isEnabled = false
                }
            }
            bottomSheetPhoneButton!!.setOnClickListener {
                if (currentParcel?.phoneNumber != null) {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:" + currentParcel?.phoneNumber)
                        startActivity(callIntent)
                    } catch (activityException: ActivityNotFoundException) {
                    }
                }
            }

            setParcelInformation(activity as AppCompatActivity)
            startGPSService()

            v.goto_launchpad.setOnClickListener { view ->
                activity!!.onBackPressed()
            }

            v.fab.setOnClickListener {

                if (isOpen) {
                    /* finish all open interactions */
                    finishSetNextParkingArea()
                    finishPedestrianRouting()
                    collapseFloatingActionButton(v)
                } else {
                    val courier = CourierRepository.shared.getCourier()
                    if (courier?.helpMode!!) {
                        v.textview_parkinglocation.startAnimation(fadeIn)
                        v.textview_vanlocation.startAnimation(fadeIn)
                        v.textview_deliverylocation.startAnimation(fadeIn)
                    }

                    v.fab_parkinglocation.startAnimation(fabOpen)
                    v.fab_vanlocation.startAnimation(fabOpen)
                    v.fab_deliverylocation.startAnimation(fabOpen)
                    v.fab.startAnimation(fabRotateClockwise)

                    v.fab_deliverylocation.isClickable = true
                    v.fab_vanlocation.isClickable = true
                    v.fab_parkinglocation.isClickable = true

                    isOpen = true
                }
            }

            /* Parcel Card */
            bottomSheetLinearLayout = v.bottom_sheet
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLinearLayout)
            /* used for swiping button (do not collapse parcel card while button swipes) */
            var disabledCollapse = false

            v.topPanel.setOnTouchListener { _, _ ->
                disabledCollapse = false
                false
            }

            /* Set swipe listener to SwipeButton */
            val swipeButtonExpandedListener = object : SwipeButton.OnSwipeButtonListener {
                override fun OnSwipeButtonConfirm(v: View?) {
                    val currentParcel = ParcelRepository.shared.getCurrentParcel()
                    if (currentParcel != null) {
                        GamificationMode.run(context!!)
                        DynamicContent.reset()
                        SizeDependentWaiting.run(FragmentRepo.mapFragmentOld!!)
                        api.confirmParcelDeliverySuccess(currentParcel.id)
                    } else {
                        Toast.createToast(getString(R.string.error_no_parcel_available))
                    }
                }

                override fun OnSwipeButtonDecline(v: View?) {
                    val currentParcel = ParcelRepository.shared.getCurrentParcel()
                    if (currentParcel != null) {
                        GamificationMode.run(context!!)
                        DynamicContent.reset()
                        SizeDependentWaiting.run(FragmentRepo.mapFragmentOld!!)

                        api.confirmParcelDeliveryFailure(currentParcel.id)
                    } else {
                        Toast.createToast(getString(R.string.error_no_parcel_available))
                    }
                }

                override fun onSwipeButtonMoved(v: View) {
                    disabledCollapse = true
                }

                override fun OnSwipeButtonFaded(v: View?) {
                    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            val swipeButton = v.swipe_btn as SwipeButton
            swipeButton.setSwipeListener(swipeButtonExpandedListener)

            /* Set on touch listener for Pull Line Button */
            v.bottom_sheet_pull_btn.setOnClickListener {
                if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
                }
                if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    v.fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset)
                        .setDuration(0).start()

                    if (isOpen) {
                        collapseFloatingActionButton(v)
                    }
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    v.fab.isClickable = newState != BottomSheetBehavior.STATE_EXPANDED
                    if (disabledCollapse) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
            })
        }

        /* floating action buttons */
        v.fab_vanlocation.setOnClickListener {
        }

        v.fab_deliverylocation.setOnClickListener {
        }

        v.fab_parkinglocation.setOnClickListener {

            if (ParcelRepository.shared.getCurrentParcel() != null) {
                if (wasclicked && !routeShown) {

                    /* Second Interaction Step */
                    showDialogOK(getString(R.string.select_next_parking_area),
                        DialogInterface.OnClickListener { _, which ->

                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    fab_parkinglocation.setImageResource(R.drawable.ic_navigation_grey_24dp)
                                    routeShown = true
                                }

                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                        })
                } else if (!wasclicked) {
                    /* First Interaction Step */
                    finishSetNextParkingArea()
                    finishGetVanLocation()
                    finishPedestrianRouting()

                    wasclicked = true

                    Toast.createToast(getString(R.string.parking_area_confirmation))


                    fab_parkinglocation.setImageResource(R.drawable.ic_custom_parker_confirm)
                }
                if (wasclicked && routeShown) {
                    /* Last Interaction Step */
                    postNextParkingAreaToServer()
                    /* finish the Interaction and set back to initial */
                    finishSetNextParkingArea()
                }
            } else {
                Toast.createToast(getString(R.string.error_no_parcel_available))
            }
        }
        return v
    }

    /* Finishes the pedestrian routing */
    private fun finishPedestrianRouting() {
        fab_deliverylocation.setImageResource(R.drawable.ic_inbox_black_24dp)
        wasClickedDeliveryLocation = false
        deliveryRouteShown = false

    }

    fun hideBottomSheetFromOutSide(event: MotionEvent) {
        if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            val outRect = Rect()
            bottomSheetLinearLayout!!.getGlobalVisibleRect(outRect)
            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt()))
                bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }


    /* Collapes FloatingActionButton e.g. after Parcel confirmation */
    private fun collapseFloatingActionButton(v: View) {
        val courier = CourierRepository.shared.getCourier()
        if (courier?.helpMode!!) {
            v.textview_parkinglocation.startAnimation(fadeOut)
            v.textview_vanlocation.startAnimation(fadeOut)
            v.textview_deliverylocation.startAnimation(fadeOut)
        }

        v.fab_parkinglocation.startAnimation(fabClose)
        v.fab_vanlocation.startAnimation(fabClose)
        v.fab_deliverylocation.startAnimation(fabClose)
        v.fab.startAnimation(fabRotateAnticlockwise)

        v.fab_deliverylocation.isClickable = false
        v.fab_vanlocation.isClickable = false
        v.fab_parkinglocation.isClickable = false

        isOpen = false
    }

    /* Sets parcel information in the bottom sheet */
    fun setParcelInformation(con: AppCompatActivity) {
        this.currentParcel = ParcelRepository.shared.getCurrentParcel()
        if (currentParcel?.address == null) {
            bottomSheetStreetName?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetStreetName?.text = currentParcel?.address
        }

        if (currentParcel?.additionalAddressInformation == null) {
            bottomSheetStreetNameAdditionalInformation?.text = con.getString(R.string.no_data_available)
        } else {
            bottomSheetStreetNameAdditionalInformation?.text = currentParcel?.additionalAddressInformation
        }


        if (currentParcel?.nameOfRecipient == null) {
            bottomSheetRecipientName?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetRecipientName?.text = currentParcel?.nameOfRecipient
        }


        if (currentParcel?.additionalRecipientInformation == null) {
            bottomSheetRecipientNameAdditionalInformation?.text = con.getString(R.string.no_data_available)
        } else {
            bottomSheetRecipientNameAdditionalInformation?.text = currentParcel?.additionalRecipientInformation
        }

        bottomSheetPhoneButton?.isEnabled = currentParcel?.phoneNumber != null
    }


    /* Startes GPS as a service */
    private fun startGPSService() {
        val gpsService = Intent(context!!.applicationContext, GPSService::class.java)
        activity!!.startService(gpsService)
    }


    override fun onDestroy() {
        super.onDestroy()

        if (broadcastReceiver != null) {
            activity!!.unregisterReceiver(broadcastReceiver)
        }

        if (gpsService != null) {
            activity!!.stopService(gpsService)
        }
    }


    override fun onResume() {
        super.onResume()
        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {

                    val latitude = intent.extras!!.getDouble("latitude")
                    val longitude = intent.extras!!.getDouble("longitude")
                    Position.update(latitude, longitude)

                    DynamicContent.manageDynamicContent()
                    if (DynamicContent.isActivated) {
                        expandBottomSheet()
                    }

                    IntelligentDriving.manageIntelligentDrivingMode()
                    if (IntelligentDriving.isActivated) {
                        var isParcelTooLarge = false
                        if (SizeDependentWaiting.isEnabled) {
                            val parcel = ParcelRepository.shared.getCurrentParcel()
                            if (parcel != null) {
                                if (ParcelUtil.getParcelSize(parcel) == "XL") {
                                    /* Is doing nothing if parcel is too large */
                                    isParcelTooLarge = true
                                }
                            }
                        }

                        if (isParcelTooLarge == false) {
                            val nextDeliveryLocation = ParcelRepository.shared.getNextParcel()
                            var nextParkingArea = ParkingAreaUtil.getNearestParkingArea(nextDeliveryLocation)
                            if (nextParkingArea == null) {
                                nextParkingArea =
                                    ParkingAreaRepository.shared.getParkingAreaById("parkingArea_-24828111#0_0_15")
                            }
                            postNextParkingAreaToServer(nextParkingArea)
                            IntelligentDriving.reset()
                        }
                    }
                }
            }
        }
        activity?.registerReceiver(broadcastReceiver, IntentFilter("location_update"))
    }


    /* PERMISSIONS */
    @TargetApi(VanAssistConfig.TARGET_API)
    fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeeded = ArrayList<String>()
        for (p in permissions) {
            result = context!!.checkSelfPermission(p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this.activity!!,
                listPermissionsNeeded.toTypedArray(),
                MapBoxConfig.MULTIPLE_PERMISSIONS
            )
            return false

        }
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MapBoxConfig.MULTIPLE_PERMISSIONS -> {
                if (grantResults.isNotEmpty()) {
                    var permissionsDenied = ""
                    for (per in permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per

                        }

                    }
                    if (permissionsDenied != "") {
                        showDialogOK(getString(R.string.permission_alert),
                            DialogInterface.OnClickListener { _, which ->
                                when (which) {
                                    DialogInterface.BUTTON_POSITIVE -> checkPermissions()
                                    DialogInterface.BUTTON_NEGATIVE -> {
                                    }
                                }
                            })
                    } else {

                    }
                }
                return
            }
        }
    }


    /* Created by Jasmin & Raluca
     * Function that creates a dialog.
     * @param: message: The message displayed on the dialog
     * @param: okListener: The implementation for the click listener */
    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this.context)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), okListener)
            .setNegativeButton(getString(R.string.cancel), okListener)
            .create()
            .show()
    }


    private fun testTargetApi(): Boolean {
        if (Build.VERSION.SDK_INT < VanAssistConfig.TARGET_API) {
            return true
        }
        return false
    }

    /* Send next parkingArea to the server */
    private fun postNextParkingAreaToServer() {
        val api = VanAssistAPIController(activity!! as AppCompatActivity)
        /* send over parkingArea retrieved from ID from Repo */
        api.postNextParkingLocation(nextParkingArea!!.id)
    }


    /* Send next parkingArea to the server */
    fun postNextParkingAreaToServer(parkingArea: ParkingAreaEntity) {
        val api = VanAssistAPIController(activity!! as AppCompatActivity)
        api.postNextParkingLocation(parkingArea.id)
    }

    /* Finish the interaction with the map (get van) */
    private fun finishGetVanLocation() {

        wasClickedVanLocation = false
    }


    /* Finish the interaction with the map (set parkingArea) */
    private fun finishSetNextParkingArea() {

        /* reset wasClicked */
        if (wasclicked) {
            wasclicked = false
        }

        /* reset markerSelected */
        if (markerSelected) {
            markerSelected = false
        }
    }

    fun expandBottomSheet() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
