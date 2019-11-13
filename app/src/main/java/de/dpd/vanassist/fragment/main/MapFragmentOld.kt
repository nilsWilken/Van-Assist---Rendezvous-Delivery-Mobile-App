package de.dpd.vanassist.fragment.main

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.*
import android.location.Location
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import de.dpd.vanassist.R
import de.dpd.vanassist.gps.GPSService
import de.dpd.vanassist.gps.Position
import kotlinx.android.synthetic.main.bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_map_old.view.*
import de.dpd.vanassist.controls.SwipeButton
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import kotlinx.android.synthetic.main.fragment_map_old.*

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.plugins.offline.model.NotificationOptions
import com.mapbox.mapboxsdk.plugins.offline.model.OfflineDownloadOptions
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflinePlugin
import com.mapbox.mapboxsdk.plugins.offline.utils.OfflineUtils
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.database.entity.ParkingArea
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.location.LocationListeningCallback
import de.dpd.vanassist.util.parkingArea.ParkingAreaUtil
import de.dpd.vanassist.util.toast.Toast

import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*


var requestSend = false

/**
 * A simple [Fragment] subclass.
 *
 */
@Suppress("DEPRECATION")
class MapFragmentOld : androidx.fragment.app.Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener, ProgressChangeListener, NavigationListener, NavigationEventListener {


    //ParkingArea Sign Layer
    private val MARKER_SOURCE = "markers-source"
    private val MARKER_STYLE_LAYER = "markers-style-layer"
    private val MARKER_IMAGE = "custom-marker"

    //Selected Marker Layer
    private val MARKER_SOURCE_SELECTED = "markers-source-selected"
    private val MARKER_STYLE_LAYER_SELECTED = "markers-style-layer-selected"
    private val MARKER_IMAGE_SELECTED = "custom-marker-selected"

    //BottomSheet Interaction
    private var llBottomSheet: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    var bottomSheetStreetName: TextView? = null
    var bottomSheetStreetNameAdditionalInformation: TextView? = null
    var bottomSheetRecipientName: TextView? = null
    var bottomSheetRecipientNameAdditionalInformation: TextView? = null
    var bottomSheetPhoneButton: Button? = null
    var currentParcel: Parcel? = null
    var swipeButton: SwipeButton? = null
    var dialog:ProgressDialog? = null
    var currentVanPosition:Marker? = null

    //Capturing original camera position to reset
    private lateinit var originalCamPos: CameraPosition

    //parkingArea Interaction
    lateinit var parkingAreas: List<ParkingArea>
    lateinit var parkingAreaRepo: ParkingAreaRepository
    lateinit var selectedParkingArea: Feature
    private var markerSelected = false
    var destination = Point.fromLngLat(0.0, 0.0)!!

    //Animations for the floating buttons
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation
    private lateinit var fabRotateClockwise: Animation
    private lateinit var fabRotateAnticlockwise: Animation
    private var isOpen = false

    //Database Repos
    private lateinit var courierRepo: CourierRepository
    private lateinit var parcelRepo: ParcelRepository

    var broadcastReceiver: BroadcastReceiver? = null

    //LocationService
    companion object {
        var gpsService: Intent? = null
        fun newInstance(): MapFragmentOld {
            return MapFragmentOld()
//            FragmentRepo.mapFragmentOld = MapFragmentOld()
//            return FragmentRepo.mapFragmentOld as MapFragmentOld
        }
    }

    //variables for Map Object
    private lateinit var mapView: MapView
    private var geopoints = HashMap<String, ArrayList<Double>>()

    //Restricing the Map Bounds
    private val BOUND_CORNER_NW = LatLng(49.4291, 8.6598)
    private val BOUND_CORNER_SE = LatLng(49.4037, 8.7148)
    private val RESTRICTED_BOUNDS_AREA = LatLngBounds.Builder()
        .include(BOUND_CORNER_NW)
        .include(BOUND_CORNER_SE)
        .build()

    private val MULTIPLE_PERMISSIONS = 10
    private var permissions =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var locationEngine: LocationEngine
    private var callback = LocationListeningCallback(this)

    lateinit var mapboxMap: MapboxMap

    // variables for calculating and drawing a route
    private lateinit var currentRoute: DirectionsRoute

    private var navigationMapRoute: NavigationMapRoute? = null
    private lateinit var navigation : MapboxNavigation

    var markerIndex = 0
    var wasclicked = false
    var wasClickedDeliveryLocation = false
    var routeShown = false
    var delrouteShown = false
    var vehicleLocation = Point.fromLngLat(8.678421, 49.416937)!!

    /**
     * Function that prepares the map (adding geopoints on the map, zooming enabled, adding custom pins on the map)
     *
     * Implemented by Jasmin and Raluca
     */
    @SuppressLint("PrivateResource")
    override fun onMapReady(mapboxMap: MapboxMap) {

        this.mapboxMap = mapboxMap
        val style_url_light = VanAssistConfig.MAP_BOX_LIGHT_STYLE
        val style_url_dark = VanAssistConfig.MAP_BOX_DARK_STYLE
        var style_url = style_url_light
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
            style_url = style_url_dark
        }

        mapboxMap.setStyle(Style.Builder().fromUrl(style_url)) { it ->

            mapboxMap.setLatLngBoundsForCameraTarget(RESTRICTED_BOUNDS_AREA)

            //LOCATION
            val locationComponentOptions = LocationComponentOptions.builder(this.context!!)
                //.layerBelow(layerId)
                .foregroundDrawable(R.drawable.mapbox_user_icon)
                .foregroundTintColor(Color.MAGENTA)
                .bearingTintColor(Color.MAGENTA)
                .accuracyAlpha(.1F)
                .compassAnimationEnabled(true)
                .accuracyAnimationEnabled(true)
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(this.context!!, it)
                .locationComponentOptions(locationComponentOptions)
                .useDefaultLocationEngine(true)
                .build()

            val locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.isLocationComponentEnabled = true
            locationComponent.renderMode = RenderMode.COMPASS

            mapboxMap.setMaxZoomPreference(18.5)
            mapboxMap.setMinZoomPreference(8.5)

            originalCamPos = mapboxMap.cameraPosition

            mapboxMap.setOnMarkerClickListener { marker ->
                newAnimatedCamPos(
                    LatLng(marker.position.latitude, marker.position.longitude),
                    originalCamPos.zoom + 2,
                    500
                )
                return@setOnMarkerClickListener true
            }

            mapboxMap.addOnMapClickListener(this)

            val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
            val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
            locationEngine = LocationEngineProvider.getBestLocationEngine(this.context!!)
            val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build()

            offlineMap()

        }

    }

    /**
     * Created by Jasmin & Raluca
     *
     * Function that starts the navigation for the current route.
     */
    private fun startTriggerNavigation() {

        val simulateRoute = false

        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .darkThemeResId(0)
            .build()

        NavigationLauncher.startNavigation(this.activity, options)



//         val ops = NavigationViewOptions.builder()
//                .directionsRoute(currentRoute)
//                .shouldSimulateRoute(simulateRoute)
//                .navigationListener(this!!)
//                .progressChangeListener(this!!)
//                .build();
//
//        val naviView = NavigationView(this.activity!!)
//        naviView.startNavigation(ops);


    }



    /**
     * Created by Jasmin
     *
     *Function that creates the navigation between the origin and the destination point.
     *
     * @param: origin: current location
     * @param: destination: the arriving point
     */
    private fun getRoute(origin_: Point, destination_: Point, profile : String) {


        try{
        val loc = mapboxMap.locationComponent.lastKnownLocation as Location
        val bearing = loc.getBearing().toDouble()
        val tolerance = 90.0

            NavigationRoute.builder(this.context!!)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin_, bearing, tolerance)
                .profile(profile)
                .destination(destination_)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {

                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        Log.i("Navigation: ", response.code().toString())

                        if (response.body() == null) {
                            Log.d("ERROR", "No route found.")
                            return
                        } else if (response.body()!!.routes().size < 1) {

                            Log.e("ERROR", "No route found.")
                        }

                        currentRoute = response.body()!!.routes()[0]

                        //Draw route on the map
                        if (navigationMapRoute != null) {

                            navigationMapRoute!!.removeRoute()
                        } else {

                            navigationMapRoute =
                                NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute)
                        }

                        navigationMapRoute!!.addRoute(currentRoute)
                    }
                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("ERROOOOR", t.message)
                }
                })
            } catch (e: SecurityException) {
                Log.i("ERROOOR", e.toString())
            }
    }


    private fun navigate(origin_:Point, destination_:Point, walking : Boolean) {
        try {
            var profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            Log.d("Dest", destination_.toString())
            if (walking){
                profile = DirectionsCriteria.PROFILE_WALKING
            }
            else{
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            }
            getRoute(origin_,destination_, profile)

        } catch (e: SecurityException) {
            Log.i("ERROOOR", e.toString())
        }
    }


    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {

        var drawable = ContextCompat.getDrawable(context, drawableId)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            drawable = (DrawableCompat.wrap(drawable!!)).mutate()
        }

        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


    private fun addMarkers(loadedMapStyle: Style) {

        val features = ArrayList<Feature>()
        parkingAreas = parkingAreaRepo.getAll()
        val nextAutoPaID = "parkingArea_429024483#3_0_12"
        if (parkingAreas.isEmpty()) {
        } else {
            for (pa in parkingAreas) {
                Log.d("PA LAT", pa.lat.toDouble().toString())
                Log.d("PA LAT", pa.long_.toDouble().toString())
                if (pa.id != nextAutoPaID) {
                    val feat = Feature.fromGeometry(Point.fromLngLat(pa.long_.toDouble(), pa.lat.toDouble()))
                    feat.addStringProperty("PA ID", pa.id)
                    features.add(feat)
                }
            }
        }


        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
        loadedMapStyle.addSource(
            GeoJsonSource(
                MARKER_SOURCE, FeatureCollection.fromFeatures(features)
            )
        )

        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        loadedMapStyle.addLayer(
            SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                .withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(false),
                    PropertyFactory.iconImage(MARKER_IMAGE),
                    //PropertyFactory.iconOpacity(0.5f),
                    // Adjust the second number of the Float array based on the height of your marker image.
                    // This is because the bottom of the marker should be anchored to the coordinate point, rather
                    // than the middle of the marker being the anchor point on the map.
                    PropertyFactory.iconOffset(floatArrayOf(0f, -8f).toTypedArray())

                )
        )

        //TODO SPECIFY NEXT PARKING AREA HERE
        var nextParkingArea = ParkingAreaUtil.getNearestParkingArea(context!!)
        if (nextParkingArea == null) {
            nextParkingArea = parkingAreaRepo.getParcelById("parkingArea_-24828111#0_0_15")

        }
        // Create new camera position
        newCamPos(LatLng(nextParkingArea.lat.toDouble(), nextParkingArea.long_.toDouble()), mapboxMap.maxZoomLevel - 4)

        //add selected marker source
        loadedMapStyle.addSource(
            GeoJsonSource(
                MARKER_SOURCE_SELECTED,
                Feature.fromGeometry(Point.fromLngLat(nextParkingArea.long_.toDouble(), nextParkingArea.lat.toDouble()))
            )
        )

        loadedMapStyle.addLayer(
            SymbolLayer(MARKER_STYLE_LAYER_SELECTED, MARKER_SOURCE_SELECTED)
                .withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconOpacity(0.9f),
                    PropertyFactory.textField("Park Here"),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.textColor(Color.RED),
                    PropertyFactory.textOffset(floatArrayOf(0f, 0.8f).toTypedArray()),
                    PropertyFactory.textSize(11f),
                    PropertyFactory.iconImage(MARKER_IMAGE_SELECTED),
                    PropertyFactory.iconOffset(floatArrayOf(0f, -8f).toTypedArray())

                )
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_map_old, container, false)

        if (testTargetApi() || checkPermissions()) {

            this.dialog = ProgressDialog.show(context, "", getString(R.string.loading_map___), true)

            Mapbox.getInstance(
                this.context!!,
                VanAssistConfig.MAP_BOX_ACCESS_TOKEN
            )

            mapView = v.findViewById(R.id.mapView)
            mapView.onCreate(savedInstanceState)

            mapView.getMapAsync(this)

            mapView.addOnDidFinishRenderingMapListener {
                this.dialog!!.dismiss()
            }

            courierRepo = CourierRepository(activity!!)
            parkingAreaRepo = ParkingAreaRepository(activity!!)
            parcelRepo = ParcelRepository(activity!!)
            this.currentParcel = parcelRepo.getNextParcelToDeliver()

            val api = VanAssistAPIController(activity!! as AppCompatActivity)

            //declaring the animations
            fabOpen = AnimationUtils.loadAnimation(context, R.anim.fab_open)
            fabClose = AnimationUtils.loadAnimation(context, R.anim.fab_close)
            fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            fabRotateClockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_clockwise)
            fabRotateAnticlockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_anticlockwise)


            //bottom sheet
            bottomSheetStreetName = v.bottom_sheet_street_text_view as TextView
            bottomSheetStreetNameAdditionalInformation =
                v.bottom_sheet_street_additional_information_text_view as TextView
            bottomSheetRecipientName = v.bottom_sheet_recipient_name_text_view as TextView
            bottomSheetRecipientNameAdditionalInformation =
                v.bottom_sheet_recipient_name__additional_information_text_view as TextView
            bottomSheetPhoneButton = v.bottom_sheet_phone_button as Button

            val courierRepo = CourierRepository(context!!)
            val current = courierRepo.getCourier()

            if (current?.darkMode!!) {
                bottomSheetStreetName!!.setTextColor(Color.WHITE)
                bottomSheetRecipientName!!.setTextColor(Color.WHITE)
            }
            else {
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

            setParcelInformation()
            startGPSService()

            v.goto_launchpad.setOnClickListener { view ->
                activity!!.onBackPressed()
            }

            v.fab.setOnClickListener {

                if (isOpen) {
                    //finish all open interactions
                    finishSetNextParking()
                    finishPedestrianRouting()

                    //collapse FAB
                    collapseFAB(v)
                } else {
                    val current = courierRepo.getCourier()
                    if (current?.mapLabel!!) {
                        v.textview_parkinglocation.startAnimation(fadeIn)
                        v.textview_vanlocation.startAnimation(fadeIn)
                        v.textview_deliverylocation.startAnimation(fadeIn)
                        v.textview_summonvan.startAnimation(fadeIn)
                    }

                    v.fab_parkinglocation.startAnimation(fabOpen)
                    v.fab_vanlocation.startAnimation(fabOpen)
                    v.fab_deliverylocation.startAnimation(fabOpen)
                    v.fab_summonvan.startAnimation(fabOpen)
                    v.fab.startAnimation(fabRotateClockwise)

                    v.fab_summonvan.isClickable = true
                    v.fab_deliverylocation.isClickable = true
                    v.fab_vanlocation.isClickable = true
                    v.fab_parkinglocation.isClickable = true

                    isOpen = true

                }
            }

            //Bottom sheet
            llBottomSheet = v.bottom_sheet
            bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet)
            var disabledCollapse =
                false        //used for swiping button (do not collapse bottom sheet while button swipes)

            v.topPanel.setOnTouchListener { _, _ ->
                disabledCollapse = false
                false
            }

            //Set swipe listener to SwipeButton
            val swipeButtonExpandedListener = object : SwipeButton.OnSwipeButtonListener {
                override fun OnSwipeButtonConfirm(v: View?) {
                    val api = VanAssistAPIController(activity!! as AppCompatActivity)
                    parcelRepo = ParcelRepository(activity!! as AppCompatActivity)
                    val nextParcel = parcelRepo.getNextParcelToDeliver()
                    if(nextParcel!= null) {
                        val parcelId = nextParcel.id
                        api.confirmParcelDeliverySuccess(parcelId)
                    }
                    Toast.createToast(getString(R.string.error_no_parcel_available))
                }

                override fun OnSwipeButtonDecline(v: View?) {
                    val api = VanAssistAPIController(activity!! as AppCompatActivity)
                    parcelRepo = ParcelRepository(activity!! as AppCompatActivity)
                    val nextParcel = parcelRepo.getNextParcelToDeliver()
                    if(nextParcel!= null) {
                        val parcelId = nextParcel.id
                        api.confirmParcelDeliveryFailure(parcelId)
                    }
                    Toast.createToast(getString(R.string.error_no_parcel_available))
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

            //Set on touch listener for Pull Line Button
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
                        collapseFAB(v)
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

        //floating action buttons

        v.fab_vanlocation.setOnClickListener {
            showVanLocation(mapboxMap.maxZoomLevel - 2, false)
        }

        v.fab_deliverylocation.setOnClickListener {
            showNextDeliveryLocation(mapboxMap.maxZoomLevel - 2, false)
        }

        v.fab_parkinglocation.setOnClickListener {

            if(parcelRepo.getNextParcelToDeliver() != null) {
                if (wasclicked && !routeShown) {

                    //Second Interaction Step
                    //TODO CHANGE TO DIALOG BUILDER TO SET CUSTOM NAMES FOR ACCEPT OR DECLINE
                    showDialogOK(getString(R.string.select_next_parking_area),
                        DialogInterface.OnClickListener { _, which ->

                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    // strip layers and sources
                                    mapboxMap.style?.removeLayer(MARKER_STYLE_LAYER)
                                    mapboxMap.style?.removeSource(MARKER_SOURCE)
                                    fab_parkinglocation.setImageResource(R.drawable.ic_navigation_grey_24dp)
                                    routeShown = true
                                    navigate(vehicleLocation,destination, false)
                                }

                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                        })
                } else if (!wasclicked) {
                    //First Interaction Step
                    finishSetNextParking()
                    wasclicked = true
                    mapboxMap.style?.removeLayer(MARKER_STYLE_LAYER_SELECTED)
                    mapboxMap.style?.removeSource(MARKER_SOURCE_SELECTED)

                    Toast.createToast(getString(R.string.parking_area_confirmation))

                    //set Custom vehicle marker
                    val icon = IconFactory.getInstance(this.activity!!)
                    // Add the marker to the map
                    mapboxMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                            .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))
                    )

                    val latlong = LatLng(vehicleLocation.latitude(), vehicleLocation.longitude())

                    mapboxMap.getStyle {

                        //Add Marker Image
                        it.addImage(MARKER_IMAGE, getBitmapFromVectorDrawable(this.context!!, R.drawable.alpha_p_circle))

                        //Add Selected Marker Image
                        it.addImage(
                            MARKER_IMAGE_SELECTED,
                            BitmapFactory.decodeResource(this.resources, R.drawable.ic_custom_parker_pin_red)
                        )
                        addMarkers(it)
                    }

                    fab_parkinglocation.setImageResource(R.drawable.ic_custom_parker_confirm)
                }
                if (wasclicked && routeShown) {
                    //Last Interaction Step
                    postNextParkingAreaToServer()
                    //finish the Interaction and set back to initial
                    finishSetNextParking()
                }
            } else {
                Toast.createToast(getString(R.string.error_no_parcel_available))
            }
        }
        return v
    }


    private fun showVanLocation(zoom: Double, animation: Boolean) {
        val icon = IconFactory.getInstance(this.activity!!)
        mapboxMap.addMarker(
            MarkerOptions()
                .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                .title("DPD Van")
                .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))

        )
        if (!animation) {
            newCamPos(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()), zoom)
        } else {
            newAnimatedCamPos(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()), zoom, 5500)
        }
    }


    private fun showNextDeliveryLocation(zoom: Double, animation: Boolean) {
        val nextParcel = parcelRepo.getNextParcelToDeliver()
        if(nextParcel == null) {
            Toast.createToast(getString(R.string.error_no_parcel_available))
            return
        }


        val latitude = nextParcel.latitude.toDouble()
        val longitude = nextParcel.longitude.toDouble()

        if (!wasClickedDeliveryLocation) {
            val icon = IconFactory.getInstance(this.activity!!)

            wasClickedDeliveryLocation = true

            mapboxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(latitude, longitude))
                    .title("Delivery Location")
                    .icon(icon.fromResource(R.mipmap.ic_destination_marker))
            )
            if (!animation) {
                newCamPos(LatLng(latitude, longitude), zoom)
            } else {
                newAnimatedCamPos(LatLng(latitude, longitude), zoom, 5500)
            }
            // change button to a navigation sign
            fab_deliverylocation.setImageResource(R.drawable.ic_navigation_grey_24dp)
        }
        else if (wasClickedDeliveryLocation && !delrouteShown ){
            // confirm routing to delivery location from GPS AKA Some static location for now
            try {
                val employeePosition = Point.fromLngLat(
                    mapboxMap.locationComponent.lastKnownLocation!!.longitude,
                    mapboxMap.locationComponent.lastKnownLocation!!.latitude
                )
                val delLocation = Point.fromLngLat(longitude, latitude)
                navigate(employeePosition, delLocation, true)
                delrouteShown = true
                fab_deliverylocation.setImageResource(R.drawable.ic_custom_parker_confirm)
            } catch (e: SecurityException) {
                Log.i("ERROOOOOOR NONONOOOO", e.toString())
            }
        }
        else if (wasClickedDeliveryLocation && delrouteShown){
            startTriggerNavigation()
        }
    }


    private fun finishPedestrianRouting(){
        fab_deliverylocation.setImageResource(R.drawable.ic_inbox_black_24dp)
        wasClickedDeliveryLocation = false
        delrouteShown = false

    }

    private fun newAnimatedCamPos(pos: LatLng, zoom: Double, durationMS: Int) {
        val position = CameraPosition.Builder()
            .target(pos) // Sets the new camera position
            .zoom(zoom) // Sets the zoom
            .bearing(0.0) // Rotate the camera
            .tilt(30.0) // Set the camera tilt
            .build() // Creates a CameraPosition from the builder

        // mapboxMap.animateCamera(CameraUpdateFactory
        //  .newCameraPosition(position), durationMS);

        mapboxMap.easeCamera(
            CameraUpdateFactory
                .newCameraPosition(position), durationMS
        )
    }

    fun updateVanLocation(point: Point, zoom: Double) {
        removeDrivingPositionOfDPDVan()
        this.vehicleLocation = point
        showVanLocation(zoom, true)
    }

    fun removeDrivingPositionOfDPDVan() {
        //remove old position
        for (marker in mapboxMap.markers) {
            if(marker.title == "DPD Van Driving") {
                mapboxMap.removeMarker(marker)
            }
        }
    }

    fun addParkingLocationWhenVanStartDriving() {
        val icon = IconFactory.getInstance(this.activity!!)
        var markerIsSet = false
        for(marker in mapboxMap.markers) {
            if (marker.title == "destination") {
                markerIsSet = true
            }
        }

        if(markerIsSet == false) {
            mapboxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(this.destination.latitude(), this.destination.longitude()))
                    .title("destination")
                    .icon(icon.fromBitmap(getBitmapFromVectorDrawable(this.context!!, R.drawable.alpha_p_circle)))
            )
        }
    }

    fun removeParkingLocationWhenVanHasParked() {
        for(marker in mapboxMap.markers)
            if(marker.title == "destination") {
                mapboxMap.removeMarker(marker)
            }
    }


    fun updateVanLocationWithoutZoom(point: Point) {
        this.vehicleLocation = point
        val icon = IconFactory.getInstance(this.activity!!)
        if(this.currentVanPosition != null) {
            this.currentVanPosition!!.remove()
        }
        removeDrivingPositionOfDPDVan()
        mapboxMap.addMarker(MarkerOptions()
            .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
            .title("DPD Van Driving")
            .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped)))
    }

    private fun newCamPos(target: LatLng, zoom: Double) {
        //create mew Cam Pos
        val cameraPosition = CameraPosition.Builder()
            .target(target)
            .zoom(zoom)
            .build()

        // Move camera to new position
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun newCamPos(cameraPosition: CameraPosition) {
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun hideBottomSheetFromOutSide(event: MotionEvent) {
        Log.i("fragment", "this was called")
        if (bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            val outRect = Rect()
            llBottomSheet!!.getGlobalVisibleRect(outRect)
            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt()))
                bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun collapseFAB(v: View) {
        val current = courierRepo.getCourier()
        if (current?.mapLabel!!) {
            v.textview_parkinglocation.startAnimation(fadeOut)
            v.textview_vanlocation.startAnimation(fadeOut)
            v.textview_deliverylocation.startAnimation(fadeOut)
            v.textview_summonvan.startAnimation(fadeOut)
        }

        v.fab_parkinglocation.startAnimation(fabClose)
        v.fab_vanlocation.startAnimation(fabClose)
        v.fab_deliverylocation.startAnimation(fabClose)
        v.fab_summonvan.startAnimation(fabClose)
        v.fab.startAnimation(fabRotateAnticlockwise)

        v.fab_summonvan.isClickable = false
        v.fab_deliverylocation.isClickable = false
        v.fab_vanlocation.isClickable = false
        v.fab_parkinglocation.isClickable = false

        isOpen = false
    }

    fun setParcelInformation() {
        this.currentParcel = parcelRepo.getNextParcelToDeliver()
        if (currentParcel?.address == null) {
            bottomSheetStreetName?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetStreetName?.text = currentParcel?.address
        }

        if (currentParcel?.additionalAddressInformation == null) {
            bottomSheetStreetNameAdditionalInformation?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetStreetNameAdditionalInformation?.text = currentParcel?.additionalAddressInformation
        }


        if (currentParcel?.nameOfRecipient == null) {
            bottomSheetRecipientName?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetRecipientName?.text = currentParcel?.nameOfRecipient
        }


        if (currentParcel?.additionalRecipientInformation == null) {
            bottomSheetRecipientNameAdditionalInformation?.text = getString(R.string.no_data_available)
        } else {
            bottomSheetRecipientNameAdditionalInformation?.text = currentParcel?.additionalRecipientInformation
        }

        if (currentParcel?.phoneNumber != null) {
            bottomSheetPhoneButton?.isEnabled = true
        } else {
            bottomSheetPhoneButton?.isEnabled = false
        }
    }


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

                    Position.latitude = latitude
                    Position.longitude = longitude
                }
            }
        }
        activity?.registerReceiver(broadcastReceiver, IntentFilter("location_update"))
    }

    //PERMISSIONS

    @TargetApi(23)
    fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeeded = ArrayList<String>()
        for (p in permissions) {
            result = context!!.checkSelfPermission(p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.d("INFO", "Log here")
                listPermissionsNeeded.add(p)
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this.activity!!,
                listPermissionsNeeded.toTypedArray(),
                MULTIPLE_PERMISSIONS
            )
            Log.d("INFO", "Still need permissions")
            return false

        }
        Log.d("INFO", "All permissions granted")
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MULTIPLE_PERMISSIONS -> {
                if (grantResults.isNotEmpty()) {
                    var permissionsDenied = ""
                    for (per in permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per

                        }

                    }
                    if (permissionsDenied != "") {
                        Log.d("INFO", "All permissions denied")
                        Log.d("INFO", permissionsDenied)

                        showDialogOK("Writing and Location Services Permission required for this app. Do you want to change Permissions",
                            DialogInterface.OnClickListener { dialog, which ->
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

    /**
     * Created by Jasmin & Raluca
     *
     * Function that creates a dialog.
     *
     * @param: message: The message displayed on the dialog
     * @param: okListener: The implementation for the click listener
     */
    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this.context)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", okListener)
            .create()
            .show()
    }

    private fun testTargetApi(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            return true
        }
        return false
    }

    private fun postNextParkingAreaToServer() {
        // instantiate API
        val api = VanAssistAPIController(activity!! as AppCompatActivity)
        // send over parkingArea retrieved from ID from Repo
        api.postNextParkingLocation(selectedParkingArea.getStringProperty("PA ID"))

    }

    override fun onMapClick(point: LatLng): Boolean {
        if (wasclicked) {
            val style = mapboxMap.style
            if (style != null) {

                val selectedMarkerSymbolLayer = style.getLayer(MARKER_STYLE_LAYER_SELECTED) as SymbolLayer

                val pixel = mapboxMap.projection.toScreenLocation(point)
                val features = mapboxMap.queryRenderedFeatures(pixel, MARKER_STYLE_LAYER)
                val selectedFeature = mapboxMap.queryRenderedFeatures(
                    pixel, MARKER_STYLE_LAYER_SELECTED
                )

                if (selectedFeature.size > 0 && markerSelected) {
                    return false
                }

                if (features.isEmpty()) {
                    if (markerSelected) {
                        deselectMarker(selectedMarkerSymbolLayer)
                    }
                    return false
                }

                val mutableList: MutableList<Feature> = arrayListOf()
                mutableList.add(Feature.fromGeometry(features[0].geometry()))

                val coorJSON = JSONObject(features[0].geometry()!!.toJson())
                val coord = coorJSON.getJSONArray("coordinates")

                Log.d("FEATURES", features[0].toString())
                selectedParkingArea = features[0]

                val long_ = coord.getDouble(0)
                val lat_ = coord.getDouble(1)
                destination = Point.fromLngLat(long_, lat_)
                newAnimatedCamPos(LatLng(lat_, long_), originalCamPos.zoom + 2, 500)

                val source: GeoJsonSource = style.getSourceAs(MARKER_SOURCE_SELECTED)!!
                source.setGeoJson(FeatureCollection.fromFeatures(mutableList))

                if (markerSelected) {
                    deselectMarker(selectedMarkerSymbolLayer)
                }
                if (features.size > 0) {
                    selectMarker(selectedMarkerSymbolLayer)
                }
            }
        }
        return true
    }


    private fun deselectMarker(iconLayer: SymbolLayer) {
        val markerAnimator = ValueAnimator()
        markerAnimator.setObjectValues(2f, 1f)
        markerAnimator.duration = 300
        markerAnimator.addUpdateListener {
            iconLayer.setProperties(PropertyFactory.iconSize(it.animatedValue as Float))
        }
        markerAnimator.start()
        markerSelected = false
    }


    private fun selectMarker(iconLayer: SymbolLayer) {

        val markerAnimator = ValueAnimator()
        markerAnimator.setObjectValues(1f, 2f)
        markerAnimator.duration = 300
        markerAnimator.addUpdateListener {
            iconLayer.setProperties(PropertyFactory.iconSize(it.animatedValue as Float))
        }
        markerAnimator.start()
        markerSelected = true

    }

    private fun finishGetVanLocation() {

        //Strip markers
        for (m in mapboxMap.markers) {
            mapboxMap.removeMarker(m)
        }

        newCamPos(originalCamPos)


    }

    private fun finishSetNextParking() {

        // set wasclicked back
        if (wasclicked) {
            wasclicked = false
        }

        // set wasclicked back
        if (markerSelected) {
            markerSelected = false
        }

        // strip all layers and sources
        val style = mapboxMap.style
        if (style != null) {
            // strip layers and sources
            mapboxMap.style?.removeLayer(MARKER_STYLE_LAYER)
            mapboxMap.style?.removeLayer(MARKER_STYLE_LAYER_SELECTED)
            mapboxMap.style?.removeSource(MARKER_SOURCE)
            mapboxMap.style?.removeSource(MARKER_SOURCE_SELECTED)
            fab_parkinglocation.setImageResource(R.drawable.ic_local_parking_black_24dp)

        }


        //set back the routes
        if (navigationMapRoute != null) {
            navigationMapRoute!!.removeRoute()
            routeShown = false
        }


        //strip all markers
        for (m in mapboxMap.markers) {
            mapboxMap.removeMarker(m)
        }

        newCamPos(originalCamPos)

    }

    /**
     * Created by Jasmin & Raluca
     *
     * Function that includes offline navigation through the map
     */
    private fun offlineMap() {

        //OFFLINE
        val offlineManager = OfflineManager.getInstance(this.activity!!)

        //Create bonding box for offline region
        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(49.4291, 8.6598)) //NW
            .include(LatLng(49.4037, 8.7148)) //SE
            .build()

        Log.d("STYLE", mapboxMap.style?.url.toString())
        //define offline region
        val definition = OfflineTilePyramidRegionDefinition(
            mapboxMap.style?.url,
            latLngBounds,
            8.5,
            18.5,
            this.resources.displayMetrics.density
        )

        //metadata
        //implementation uses JSON to store Neuenheim as the offline region name
        var metadata: ByteArray?
        try {

            val jsonObject = JSONObject()
            jsonObject.put("Region", "Neuenheim")
            val json = jsonObject.toString()
            metadata = json.toByteArray(charset("UTF-8"))
        } catch (e: Exception) {

            Log.e("ERRORRRR", "Failed to encode metadata: " + e.message)
            metadata = null
        }


        //checking if the region is already downloaded
        var regionDownloaded = true
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                regionDownloaded = !(offlineRegions == null || offlineRegions.isEmpty())
            }

            override fun onError(error: String?) {
                Log.e("ERROR", "Error: $error")
            }

        })


        offlineManager.createOfflineRegion(definition, metadata!!, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion?) {
                offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)

                offlineRegion?.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        // Notify if offline region exceeds maximum tile count
                        Log.e("ERROR", "Mapbox tile count limit exceeded: $limit")
                    }

                    override fun onStatusChanged(status: OfflineRegionStatus?) {

                        // Calculate the download percentage
                        val percentage = if (status?.requiredResourceCount!! >= 0)
                            100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0

                        if (status.isComplete) {
                            // Download complete
                            Log.d("ERROR", "Region downloaded successfully.")
                        } else if (status.isRequiredResourceCountPrecise) {
                            Log.d("ERROR", percentage.toString())
                        }
                    }

                    override fun onError(error: OfflineRegionError?) {

                        // If an error occurs, print to logcat
                        Log.e("ERROR", "onError reason: " + error?.reason)
                        Log.e("ERROR", "onError message: " + error?.message)
                    }
                })
            }

            override fun onError(error: String?) {
                Log.e("ERROR", "Error: $error")
            }
        })

        // Customize the download notification's appearance
        val notificationOptions = NotificationOptions.builder(this.activity!!)
            .smallIconRes(R.drawable.mapbox_logo_icon)
            .returnActivity(this.activity!!::class.java.name)
            .build()

        if (!regionDownloaded) {
            // Start downloading the map tiles for offline use
            OfflinePlugin.getInstance(this.activity!!).startDownload(
                OfflineDownloadOptions.builder()
                    .definition(definition)
                    .metadata(OfflineUtils.convertRegionName("Neuenheim"))
                    .notificationOptions(notificationOptions)
                    .build()
            )
        }

        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                // Get the region bounds and zoom and move the camera.
                val bounds = (offlineRegions!![0].definition as OfflineTilePyramidRegionDefinition).bounds
                Log.d("BOUNDS", bounds.toString())

                val regionZoom = (offlineRegions[0].definition as OfflineTilePyramidRegionDefinition).minZoom
                Log.d("BOUNDS", regionZoom.toString())

            }

            override fun onError(error: String?) {
                Log.e("ERROR", "Error: $error")
            }

        })
    }


    //NAVIGATION EVENT LISTENER
    override fun onRunning(running: Boolean) {
        if (running) {
           Log.d("onRunning:", "Started")
        } else {
            Log.d("onRunning:", "Stopped")
        }
    }

    override fun onNavigationFinished() {
        Log.d("onRunning:", "Finished")
    }

    override fun onNavigationRunning() {
        Log.d("onRunning:", "Started")
    }

    override fun onCancelNavigation() {
        Log.d("onRunning:", "Canceled")
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
