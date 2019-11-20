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
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute

import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.config.MapBoxConfig
import de.dpd.vanassist.config.ParkingAreaConfig
import de.dpd.vanassist.config.SimulationConfig
import de.dpd.vanassist.config.VanAssistConfig
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.entity.ParkingAreaEntity
import de.dpd.vanassist.database.entity.VanEntity
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.database.repository.ParkingAreaRepository
import de.dpd.vanassist.database.repository.VanRepository
import de.dpd.vanassist.intelligence.dynamicContent.DynamicContent
import de.dpd.vanassist.intelligence.gamification.GamificationMode
import de.dpd.vanassist.intelligence.intelligentDriving.IntelligentDriving
import de.dpd.vanassist.intelligence.sizeDependentWaiting.SizeDependentWaiting
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.parcel.ParcelUtil
import de.dpd.vanassist.util.parkingArea.ParkingAreaUtil
import de.dpd.vanassist.util.toast.Toast

import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*


var requestSend = false

/* A simple [Fragment] subclass. */
@Suppress("DEPRECATION")
class MapFragmentOld : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {

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
    private var currentVanPosition: Marker? = null

    /* Capturing original camera position to reset */
    private lateinit var originalCamPos: CameraPosition

    /* parkingArea Interaction */
    private lateinit var parkingAreas: List<ParkingAreaEntity>
    private var selectedParkingArea: Feature? = null
    var nextParkingArea : ParkingAreaEntity? = null
    private var markerSelected = false
    var destination = Point.fromLngLat(0.0, 0.0)!!

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

    /* variables for Map Object */
    private lateinit var mapView: MapView

    /* Restricting the Map Bounds */
    private val RESTRICTED_BOUNDS_AREA = LatLngBounds.Builder()
        .include(MapBoxConfig.OFFLINE_MAP_BOUND_NORTH_WEST)
        .include(MapBoxConfig.OFFLINE_MAP_BOUND_SOUTH_EAST)
        .build()

    private var permissions =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var locationEngine: LocationEngine

    lateinit var mapBoxMap: MapboxMap

    /* variables for calculating and drawing a route */
    private lateinit var currentRoute: DirectionsRoute

    private var navigationMapRoute: NavigationMapRoute? = null

    private var wasclicked = false
    private var wasClickedDeliveryLocation = false
    private var wasClickedVanLocation = false
    private var routeShown = false
    private var deliveryRouteShown = false
    private var vehicleLocation = MapBoxConfig.DEFAULT_VEHICLE_LOCATION

    /* Implemented by Jasmin and Raluca
     * Function that prepares the map (adding geopoints on the map, zooming enabled, adding custom pins on the map) */
    @SuppressLint("PrivateResource")
    override fun onMapReady(mapboxMap: MapboxMap) {

        this.mapBoxMap = mapboxMap
        val styleUrlLight = MapBoxConfig.MAP_BOX_LIGHT_STYLE
        val styleUrlDark = MapBoxConfig.MAP_BOX_DARK_STYLE
        var styleUrl = styleUrlLight
        val courier = CourierRepository.shared.getCourier()

        if (courier?.darkMode!!) {
            styleUrl = styleUrlDark
        }

        mapboxMap.setStyle(Style.Builder().fromUrl(styleUrl)) {

            mapboxMap.setLatLngBoundsForCameraTarget(RESTRICTED_BOUNDS_AREA)

            val locationComponentOptions = LocationComponentOptions.builder(this.context!!)
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

            mapboxMap.setMaxZoomPreference(MapBoxConfig.MAX_ZOOM)
            mapboxMap.setMinZoomPreference(MapBoxConfig.MIN_ZOOM)

            originalCamPos = mapboxMap.cameraPosition

            mapboxMap.setOnMarkerClickListener { marker ->
                newAnimatedCamPos(
                    LatLng(marker.position.latitude, marker.position.longitude),
                    originalCamPos.zoom + 2,
                    MapBoxConfig.SET_MARKER_DURATION_IN_MS
                )
                return@setOnMarkerClickListener true
            }

            mapboxMap.addOnMapClickListener(this)

            locationEngine = LocationEngineProvider.getBestLocationEngine(this.context!!)

            LocationEngineRequest.Builder(MapBoxConfig.DEFAULT_INTERVAL_IN_MS)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .setMaxWaitTime(MapBoxConfig.DEFAULT_MAX_WAIT_TIME)
                .build()

            offlineMap()
        }
    }


    /* Created by Jasmin & Raluca
     * Function that starts the navigation for the current route. */
    private fun startTriggerNavigation() {

        val simulateRoute = false

        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .build()

        NavigationLauncher.startNavigation(this.activity, options)
    }


    /* Created by Jasmin
     * Function that creates the navigation between the origin and the destination point.
     * @param: origin: current location
     * @param: destination: the arriving point */
    private fun getRoute(origin_: Point, destination_: Point, profile: String) {

        try {
            val loc = mapBoxMap.locationComponent.lastKnownLocation as Location
            val bearing = loc.getBearing().toDouble()
            NavigationRoute.builder(this.context!!)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin_, bearing, MapBoxConfig.NAVIGATION_TOLERANCE)
                .profile(profile)
                .destination(destination_)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {

                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.body() == null) {
                            return
                        } else if (response.body()!!.routes().size < 1) {
                        }

                        currentRoute = response.body()!!.routes()[0]

                        /* Draw route on the map */
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute =
                                NavigationMapRoute(null, mapView, mapBoxMap, R.style.NavigationMapRoute)
                        }
                        navigationMapRoute!!.addRoute(currentRoute)
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    }
                })
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    private fun navigate(origin_: Point, destination_: Point, walking: Boolean) {
        try {
            var profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            if (walking) {
                profile = DirectionsCriteria.PROFILE_WALKING
            }
            getRoute(origin_, destination_, profile)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


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


    /* The markers are added in this method */
    private fun addMarkers(loadedMapStyle: Style) {
        val features = ArrayList<Feature>()
        parkingAreas = ParkingAreaRepository.shared.getAll()
        val nextAutoPaID = "parkingArea_429024483#3_0_12"
        if (parkingAreas.isEmpty()) {
        } else {
            for (pa in parkingAreas) {
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
                MapBoxConfig.MARKER_SOURCE, FeatureCollection.fromFeatures(features)
            )
        )

        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        loadedMapStyle.addLayer(
            SymbolLayer(MapBoxConfig.MARKER_STYLE_LAYER, MapBoxConfig.MARKER_SOURCE)
                .withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(false),
                    PropertyFactory.iconImage(MapBoxConfig.MARKER_IMAGE),

                    /* Adjust the second number of the Float array based on the height of your marker image.
                       This is because the bottom of the marker should be anchored to the coordinate point, rather
                       than the middle of the marker being the anchor point on the map. */
                    PropertyFactory.iconOffset(MapBoxConfig.ICON_OFFSET)
                )
        )

        /* Search for best parking area, if no one is found -> use default */
        val nextDeliveryLocation = ParcelRepository.shared.getCurrentParcel()
        nextParkingArea = ParkingAreaUtil.getNearestParkingArea(nextDeliveryLocation)
        if (nextParkingArea == null) {
            nextParkingArea = ParkingAreaRepository.shared.getParkingAreaById(ParkingAreaConfig.DEFAULT_PARKING_AREA)
        }

        /* set nextParkingArea as default destination */
        destination = Point.fromLngLat(nextParkingArea!!.long_.toDouble(), nextParkingArea!!.lat.toDouble())

        /* Create new camera position */
        newCamPos(LatLng(nextParkingArea!!.lat.toDouble(), nextParkingArea!!.long_.toDouble()), mapBoxMap.maxZoomLevel - 4)

        /* add selected marker source */
        loadedMapStyle.addSource(
            GeoJsonSource(
                MapBoxConfig.MARKER_SOURCE_SELECTED,
                Feature.fromGeometry(Point.fromLngLat(nextParkingArea!!.long_.toDouble(), nextParkingArea!!.lat.toDouble()))
            )
        )

        loadedMapStyle.addLayer(
            SymbolLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED, MapBoxConfig.MARKER_SOURCE_SELECTED)
                .withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconOpacity(MapBoxConfig.MARKER_PROPERTY_ICON_OPACITY),
                    PropertyFactory.textField(getString(R.string.park_here_title)),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.textColor(Color.RED),
                    PropertyFactory.textOffset(MapBoxConfig.MARKER_PROPERTY_TEXT_OFFSET),
                    PropertyFactory.textSize(MapBoxConfig.MARKER_PROPERTY_TEXT_SIZE),
                    PropertyFactory.iconImage(MapBoxConfig.MARKER_IMAGE_SELECTED),
                    PropertyFactory.iconOffset(MapBoxConfig.ICON_OFFSET)

                )
        )

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    destination.latitude(),
                    destination.longitude(),
                    true
                )
            )
            SimulationConfig.isFirstVanLocationAfterSimulationStart = false
        }


        if (testTargetApi() || checkPermissions()) {

            this.dialog = ProgressDialog.show(context, "", getString(R.string.loading_map___), true)

            Mapbox.getInstance(
                this.context!!,
                MapBoxConfig.MAP_BOX_ACCESS_TOKEN
            )

            mapView = v.findViewById(R.id.mapView)
            mapView.onCreate(savedInstanceState)

            mapView.getMapAsync(this)

            mapView.addOnDidFinishRenderingMapListener {
                this.dialog!!.dismiss()
            }

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
            showVanLocation(mapBoxMap.maxZoomLevel - 2, false)
        }

        v.fab_deliverylocation.setOnClickListener {
            showNextDeliveryLocation(mapBoxMap.maxZoomLevel - 2, false)
        }

        v.fab_parkinglocation.setOnClickListener {

            if (ParcelRepository.shared.getCurrentParcel() != null) {
                if (wasclicked && !routeShown) {

                    /* Second Interaction Step */
                    showDialogOK(getString(R.string.select_next_parking_area),
                        DialogInterface.OnClickListener { _, which ->

                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    /* strip layers and sources */
                                    mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER)
                                    mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE)
                                    fab_parkinglocation.setImageResource(R.drawable.ic_navigation_grey_24dp)
                                    routeShown = true
                                    navigate(vehicleLocation, destination, false)
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
                    mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)
                    mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE_SELECTED)

                    Toast.createToast(getString(R.string.parking_area_confirmation))

                    /* set Custom vehicle marker */
                    val icon = IconFactory.getInstance(this.activity!!)
                    /* Add the marker to the map */
                    mapBoxMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                            .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))
                    )

                    mapBoxMap.getStyle {

                        /* Add Marker Image */
                        it.addImage(
                            MapBoxConfig.MARKER_IMAGE,
                            getBitmapFromVectorDrawable(this.context!!, R.drawable.alpha_p_circle)
                        )

                        /* Add Selected Marker Image */
                        it.addImage(
                            MapBoxConfig.MARKER_IMAGE_SELECTED,
                            BitmapFactory.decodeResource(this.resources, R.drawable.ic_custom_parker_pin_red)
                        )

                        addMarkers(it)
                    }

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


    /* Manages the display of the van location in the map */
    private fun showVanLocation(zoom: Double, animation: Boolean) {
        val icon = IconFactory.getInstance(this.activity!!)
        wasClickedVanLocation = true
        mapBoxMap.addMarker(
            MarkerOptions()
                .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                .title("DPD Van")
                .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))

        )
        if (!animation) {
            newCamPos(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()), zoom)
        } else {
            newAnimatedCamPos(
                LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()),
                zoom,
                VanAssistConfig.VAN_ARRIVAL_ZOOM_DURATION
            )
        }
    }


    /* Manages the display of the next delivery location in the map */
    private fun showNextDeliveryLocation(zoom: Double, animation: Boolean) {
        val nextParcelToDeliver = ParcelRepository.shared.getCurrentParcel()
        if (nextParcelToDeliver == null) {
            Toast.createToast(getString(R.string.error_no_parcel_available))
            return
        }

        val latitude = nextParcelToDeliver.latitude.toDouble()
        val longitude = nextParcelToDeliver.longitude.toDouble()

        if (!wasClickedDeliveryLocation) {
            finishSetNextParkingArea()
            finishPedestrianRouting()

            val icon = IconFactory.getInstance(this.activity!!)

            wasClickedDeliveryLocation = true

            if(wasClickedVanLocation){
                mapBoxMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                        .title("DPD Van")
                        .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))

                )
            }

            mapBoxMap.addMarker(
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
            /* change button to a navigation sign */
            fab_deliverylocation.setImageResource(R.drawable.ic_navigation_grey_24dp)

        } else if (wasClickedDeliveryLocation && !deliveryRouteShown) {

            /* confirm routing to delivery location from GPS AKA Some static location for now */
            try {
                val employeePosition = Point.fromLngLat(
                    mapBoxMap.locationComponent.lastKnownLocation!!.longitude,
                    mapBoxMap.locationComponent.lastKnownLocation!!.latitude
                )
                val delLocation = Point.fromLngLat(longitude, latitude)
                navigate(employeePosition, delLocation, true)
                deliveryRouteShown = true
                fab_deliverylocation.setImageResource(R.drawable.ic_custom_parker_confirm)

            } catch (e: SecurityException) {
                e.printStackTrace()
            }

        } else if (wasClickedDeliveryLocation && deliveryRouteShown) {

            startTriggerNavigation()
        }
    }


    /* Finishes the pedestrian routing */
    private fun finishPedestrianRouting() {
        fab_deliverylocation.setImageResource(R.drawable.ic_inbox_black_24dp)
        wasClickedDeliveryLocation = false
        deliveryRouteShown = false

    }


    /* Animates the camera position e.g. for to the van after it has arrived in the parking Area */
    private fun newAnimatedCamPos(pos: LatLng, zoom: Double, durationMS: Int) {
        val position = CameraPosition.Builder()
            /* Sets the new camera position */
            .target(pos)
            /* Sets the zoom */
            .zoom(zoom)
            /* Rotate the camera */
            .bearing(0.0)
            /* Set the camera tilt */
            .tilt(30.0)
            /* Creates a CameraPosition from the builder */
            .build()

        mapBoxMap.easeCamera(
            CameraUpdateFactory
                .newCameraPosition(position), durationMS
        )
    }


    /* Updates the van location (approx. every 3 seconds */
    fun updateVanLocation(point: Point, zoom: Double) {
        removeDrivingPositionOfDPDVan()
        this.vehicleLocation = point
        showVanLocation(zoom, true)
    }


    /* Removes the old position of the van in the map*/
    private fun removeDrivingPositionOfDPDVan() {
        for (marker in mapBoxMap.markers) {
            if (marker.title == MapBoxConfig.MARKER_TITLE_VAN_IS_DRIVING) {
                mapBoxMap.removeMarker(marker)
            }
        }
    }


    /* Shows the target parking position on the map during the navigation of the van */
    fun addParkingLocationWhenVanStartDriving() {
        val icon = IconFactory.getInstance(this.activity!!)
        var markerIsSet = false
        for (marker in mapBoxMap.markers) {
            if (marker.title == MapBoxConfig.MARKER_TITLE_DESTINATION) {
                markerIsSet = true
            }
        }

        if (markerIsSet == false) {
            mapBoxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(this.destination.latitude(), this.destination.longitude()))
                    .title(MapBoxConfig.MARKER_TITLE_DESTINATION)
                    .icon(icon.fromBitmap(getBitmapFromVectorDrawable(this.context!!, R.drawable.alpha_p_circle)))
            )
        }
    }


    /* Removes the target parkingArea after the van has arrived in the parkingArea */
    fun removeParkingLocationWhenVanHasParked() {
        for (marker in mapBoxMap.markers)
            if (marker.title == MapBoxConfig.MARKER_TITLE_DESTINATION) {
                mapBoxMap.removeMarker(marker)
            }
    }


    /* Updates the van location without zooming to the van */
    fun updateVanLocationWithoutZoom(point: Point) {
        this.vehicleLocation = point
        val icon = IconFactory.getInstance(this.activity!!)
        if (this.currentVanPosition != null) {
            this.currentVanPosition!!.remove()
        }
        removeDrivingPositionOfDPDVan()
        mapBoxMap.addMarker(
            MarkerOptions()
                .position(LatLng(vehicleLocation.latitude(), vehicleLocation.longitude()))
                .title(MapBoxConfig.MARKER_TITLE_VAN_IS_DRIVING)
                .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))
        )
    }


    /* Create new Cam Pos */
    private fun newCamPos(target: LatLng, zoom: Double) {
        val cameraPosition = CameraPosition.Builder()
            .target(target)
            .zoom(zoom)
            .build()

        /* Move camera to new position */
        mapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }


    private fun newCamPos(cameraPosition: CameraPosition) {
        mapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
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
    fun setParcelInformation(con:AppCompatActivity) {
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

        if (currentParcel?.phoneNumber != null) {
            bottomSheetPhoneButton?.isEnabled = true
        } else {
            bottomSheetPhoneButton?.isEnabled = false
        }
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
                    if(IntelligentDriving.isActivated) {
                        var isParcelTooLarge = false
                        if(SizeDependentWaiting.isEnabled) {
                            val parcel = ParcelRepository.shared.getCurrentParcel()
                            if(parcel != null) {
                                if(ParcelUtil.getParcelSize(parcel) == "XL") {
                                    /* Is doing nothing if parcel is too large */
                                    isParcelTooLarge = true
                                }
                            }
                        }

                        if(isParcelTooLarge == false) {
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
        if (selectedParkingArea != null) {
            api.postNextParkingLocation(selectedParkingArea!!.getStringProperty("PA ID"))
        } else {
            api.postNextParkingLocation(nextParkingArea!!.id)
        }
    }


    /* Send next parkingArea to the server */
    fun postNextParkingAreaToServer(parkingArea: ParkingAreaEntity) {
        val api = VanAssistAPIController(activity!! as AppCompatActivity)
        api.postNextParkingLocation(parkingArea.id)
    }

    /* EventHandler for interaction with the map */
    override fun onMapClick(point: LatLng): Boolean {
        if (wasclicked) {
            val style = mapBoxMap.style
            if (style != null) {

                val selectedMarkerSymbolLayer = style.getLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED) as SymbolLayer

                val pixel = mapBoxMap.projection.toScreenLocation(point)
                val features = mapBoxMap.queryRenderedFeatures(pixel, MapBoxConfig.MARKER_STYLE_LAYER)
                val selectedFeature = mapBoxMap.queryRenderedFeatures(
                    pixel, MapBoxConfig.MARKER_STYLE_LAYER_SELECTED
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

                val geoCoordinateJSON = JSONObject(features[0].geometry()!!.toJson())
                val geoCoordinate = geoCoordinateJSON.getJSONArray("coordinates")

                selectedParkingArea = features[0]

                val longitude = geoCoordinate.getDouble(0)
                val latitude = geoCoordinate.getDouble(1)
                destination = Point.fromLngLat(longitude, latitude)
                newAnimatedCamPos(LatLng(latitude, longitude), originalCamPos.zoom + 2, MapBoxConfig.CAM_POS_ANIMATION_IN_MS)

                val source: GeoJsonSource = style.getSourceAs(MapBoxConfig.MARKER_SOURCE_SELECTED)!!
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


    /* Handles the deselection of markers */
    private fun deselectMarker(iconLayer: SymbolLayer) {
        val markerAnimator = ValueAnimator()
        markerAnimator.setObjectValues(2f, 1f)
        markerAnimator.duration = MapBoxConfig.MARKER_ANIMATION_DURATION
        markerAnimator.addUpdateListener {
            iconLayer.setProperties(PropertyFactory.iconSize(it.animatedValue as Float))
        }
        markerAnimator.start()
        markerSelected = false
    }


    /* Handles the selection of markers */
    private fun selectMarker(iconLayer: SymbolLayer) {

        val markerAnimator = ValueAnimator()
        markerAnimator.setObjectValues(1f, 2f)
        markerAnimator.duration = MapBoxConfig.MARKER_ANIMATION_DURATION
        markerAnimator.addUpdateListener {
            iconLayer.setProperties(PropertyFactory.iconSize(it.animatedValue as Float))
        }
        markerAnimator.start()
        markerSelected = true

    }


    /* Finish the interaction with the map (get van) */
    private fun finishGetVanLocation() {

        wasClickedVanLocation = false
        /* Strip markers */
        for (m in mapBoxMap.markers) {
            mapBoxMap.removeMarker(m)
        }
        newCamPos(originalCamPos)
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

        /* strip all layers and sources */
        val style = mapBoxMap.style
        if (style != null) {
            /* strip layers and sources */
            mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER)
            mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)
            mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE)
            mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE_SELECTED)
            fab_parkinglocation.setImageResource(R.drawable.ic_local_parking_black_24dp)
        }

        /* reset the routes */
        if (navigationMapRoute != null) {
            navigationMapRoute!!.removeRoute()
            routeShown = false
        }

        /* strip all markers */
        for (m in mapBoxMap.markers) {
            mapBoxMap.removeMarker(m)
        }
        newCamPos(originalCamPos)
    }


    /* Created by Jasmin & Raluca
     * Function that includes offline navigation through the map */
    private fun offlineMap() {

        val offlineManager = OfflineManager.getInstance(this.activity!!)

        /* Create bounding box for offline region */
        val latLngBounds = LatLngBounds.Builder()
            .include(MapBoxConfig.OFFLINE_MAP_BOUND_NORTH_WEST)
            .include(MapBoxConfig.OFFLINE_MAP_BOUND_SOUTH_EAST)
            .build()

        /* Define offline region */
        val definition = OfflineTilePyramidRegionDefinition(
            mapBoxMap.style?.url,
            latLngBounds,
            MapBoxConfig.MIN_ZOOM,
            MapBoxConfig.MAX_ZOOM,
            this.resources.displayMetrics.density
        )

        /* metadata */
        /* implementation uses JSON to store Neuenheim as the offline region name */
        var metadata: ByteArray?
        try {

            val jsonObject = JSONObject()
            jsonObject.put("Region", "Neuenheim")
            val json = jsonObject.toString()
            metadata = json.toByteArray(charset(VanAssistConfig.CHAR_SET))
        } catch (e: Exception) {
            metadata = null
        }

        /* checking if the region is already downloaded */
        var regionDownloaded = true
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                regionDownloaded = !(offlineRegions == null || offlineRegions.isEmpty())
            }

            override fun onError(e: String?) {}

        })

        offlineManager.createOfflineRegion(definition, metadata!!, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion?) {
                offlineRegion?.setDownloadState(OfflineRegion.STATE_ACTIVE)

                offlineRegion?.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        /* Notify if offline region exceeds maximum tile count */
                    }

                    override fun onStatusChanged(status: OfflineRegionStatus?) {}

                    override fun onError(error: OfflineRegionError?) {}
                })
            }

            override fun onError(error: String?) {}
        })

        /* Customize the download notification's appearance */
        val notificationOptions = NotificationOptions.builder(this.activity!!)
            .smallIconRes(R.drawable.mapbox_logo_icon)
            .returnActivity(this.activity!!::class.java.name)
            .build()

        if (!regionDownloaded) {
            /* Start downloading the map tiles for offline use */
            OfflinePlugin.getInstance(this.activity!!).startDownload(
                OfflineDownloadOptions.builder()
                    .definition(definition)
                    .metadata(OfflineUtils.convertRegionName("Neuenheim"))
                    .notificationOptions(notificationOptions)
                    .build()
            )
        }
    }


    fun expandBottomSheet() {
        bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
