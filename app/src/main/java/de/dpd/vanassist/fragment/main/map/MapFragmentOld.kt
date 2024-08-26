package de.dpd.vanassist.fragment.main.map

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
import android.os.Parcel
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import androidx.lifecycle.Observer

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
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.ui.route.NavigationMapRoute
import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.adapters.ParcelBottomSheetAdapter
//import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
//import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
//import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
//import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute

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
import de.dpd.vanassist.fragment.main.launchpad.VehicleProblemDialogFragment
import de.dpd.vanassist.intelligence.dynamicContent.DynamicContent
import de.dpd.vanassist.intelligence.gamification.GamificationMode
import de.dpd.vanassist.intelligence.intelligentDriving.IntelligentDriving
import de.dpd.vanassist.intelligence.sizeDependentWaiting.SizeDependentWaiting
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.parcel.ParcelUtil
import de.dpd.vanassist.util.parkingArea.ParkingAreaUtil
import de.dpd.vanassist.util.toast.Toast
import kotlinx.android.synthetic.main.bottom_sheet.view.swipe_btn
import kotlinx.android.synthetic.main.bottom_sheet_v2.view.*
import kotlinx.android.synthetic.main.parcel_information_card_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Thread.sleep
import java.lang.reflect.TypeVariable

import java.util.*
import kotlin.collections.ArrayList


var requestSend = false

/* A simple [Fragment] subclass. */
@Suppress("DEPRECATION")
class MapFragmentOld : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {

    /* PARCEL CARD INTERACTION */
    private var bottomSheetRecView: LinearLayout? = null
    private var bottomSheetSingleParcel: LinearLayout? = null
    private var bottomSheetLinearLayout: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>? = null
    private var disabledCollapse = false
    private var bottomSheetStreetName: TextView? = null
    private var bottomSheetStreetNameAdditionalInformation: TextView? = null
    private var bottomSheetRecipientName: TextView? = null
    private var bottomSheetRecipientNameAdditionalInformation: TextView? = null
    private var bottomSheetPhoneButton: Button? = null
    private var bottomSheetSwipeButton: SwipeButton? = null
    private lateinit var parcelAdapter: ParcelBottomSheetAdapter
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private var currentParcel: ParcelEntity? = null
    private var bottomSheetState = 0
    private var parkingLocationCounter = 0

    var dialog: ProgressDialog? = null
    private var currentVanPosition: Marker? = null

    /* Capturing original camera position to reset */
    private lateinit var originalCamPos: CameraPosition

    /* PARKING AREA INTERACTION */
    private lateinit var parkingAreas: List<ParkingAreaEntity>
    private var selectedParkingArea: Feature? = null
    var lastParkingArea: ParkingAreaEntity? = null
    var fSelectedParkingArea: ParkingAreaEntity? = null
    var nextParkingArea : ParkingAreaEntity? = null
    private var markerSelected = false
    var destination = Point.fromLngLat(0.0, 0.0)!!

    /* FLOATING BUTTON ANIMATIONS */
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation
    private lateinit var fabRotateClockwise: Animation
    private lateinit var fabRotateAnticlockwise: Animation
    private var isOpen = false

    private var showVanLocationOnCreation = false

    private var broadcastReceiver: BroadcastReceiver? = null

    /* LOCATION SERVICE */
    companion object {
        var gpsService: Intent? = null
        var gNextParkingArea: ParkingAreaEntity? = null
        fun newInstance(): MapFragmentOld {
            if(FragmentRepo.mapFragmentOld == null) {
                FragmentRepo.mapFragmentOld = MapFragmentOld()
            }
            return FragmentRepo.mapFragmentOld as MapFragmentOld
        }
    }

    /* MAB BOX VARIABLES */
    private lateinit var mapView: MapView

    /* MAP BOUND RESTRICTIONS */
    private val RESTRICTED_BOUNDS_AREA = LatLngBounds.Builder()
        .include(MapBoxConfig.OFFLINE_MAP_BOUND_NORTH_WEST)
        .include(MapBoxConfig.OFFLINE_MAP_BOUND_SOUTH_EAST)
        .build()

    private var permissions =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var locationEngine: LocationEngine

    /* MAP BOX */
    lateinit var mapBoxMap: MapboxMap

    /* ROUTE UTILS */
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

        //SET AND DETERMINE MAP STYLE (LIGHT VS DARK)
        val styleUrlLight = MapBoxConfig.MAP_BOX_LIGHT_STYLE
        val styleUrlDark = MapBoxConfig.MAP_BOX_DARK_STYLE
        var styleUrl = styleUrlLight
        val courier = CourierRepository.shared.getCourier()

        if (courier?.darkMode!!) {
            styleUrl = styleUrlDark
        }
        mapboxMap.setStyle(Style.Builder().fromUri(styleUrl)) {
            mapboxMap.setLatLngBoundsForCameraTarget(RESTRICTED_BOUNDS_AREA)


            val locationComponentOptions = LocationComponentOptions.builder(this.requireContext())
                .foregroundDrawable(R.drawable.mapbox_user_icon)
                .foregroundTintColor(Color.MAGENTA)
                .bearingTintColor(Color.MAGENTA)
                .accuracyAlpha(.1F)
                .compassAnimationEnabled(true)
                .accuracyAnimationEnabled(true)
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(this.requireContext(), it)
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

            this.originalCamPos = mapboxMap.cameraPosition

            mapboxMap.setOnMarkerClickListener { marker ->
                newAnimatedCamPos(
                    LatLng(marker.position.latitude, marker.position.longitude),
                    this.originalCamPos.zoom + 1,
                    MapBoxConfig.SET_MARKER_DURATION_IN_MS
                )
                return@setOnMarkerClickListener true
            }

            mapboxMap.addOnMapClickListener(this)

            this.locationEngine = LocationEngineProvider.getBestLocationEngine(this.requireContext())

            LocationEngineRequest.Builder(MapBoxConfig.DEFAULT_INTERVAL_IN_MS)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .setMaxWaitTime(MapBoxConfig.DEFAULT_MAX_WAIT_TIME)
                .build()


            /* Add Marker Image */
            it.addImage(
                MapBoxConfig.MARKER_IMAGE,
                getBitmapFromVectorDrawable(this.requireContext(), R.drawable.alpha_p_circle)
            )

            /* Add Selected Marker Image */
            it.addImage(
                MapBoxConfig.MARKER_IMAGE_SELECTED,
                BitmapFactory.decodeResource(this.resources, R.drawable.ic_custom_parker_pin_red)
            )

            val features = ArrayList<Feature>()
            this.parkingAreas = ParkingAreaRepository.shared.getAll()
            val nextAutoPaID = "parkingArea_429024483#3_0_12"
            if (!this.parkingAreas.isEmpty()) {
                for (pa in this.parkingAreas) {
                    if (pa.id != nextAutoPaID) {
                        val feat = Feature.fromGeometry(Point.fromLngLat(pa.long_.toDouble(), pa.lat.toDouble()))
                        feat.addStringProperty("PA ID", pa.id)
                        features.add(feat)
                    }
                }
            }

            /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
            it.addSource(
                GeoJsonSource(
                    MapBoxConfig.MARKER_SOURCE, FeatureCollection.fromFeatures(features)
                )
            )

            /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
            it.addLayer(
                SymbolLayer(MapBoxConfig.MARKER_STYLE_LAYER, MapBoxConfig.MARKER_SOURCE)
                    .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconImage(MapBoxConfig.MARKER_IMAGE),
                        PropertyFactory.visibility(NONE),

                        /* Adjust the second number of the Float array based on the height of your marker image.
                           This is because the bottom of the marker should be anchored to the coordinate point, rather
                           than the middle of the marker being the anchor point on the map. */
                        PropertyFactory.iconOffset(MapBoxConfig.ICON_OFFSET)
                    )
            )

            it.addSource(
                GeoJsonSource(
                    MapBoxConfig.MARKER_SOURCE_SELECTED,
                    FeatureCollection.fromFeatures(arrayListOf())
                )
            )

            it.addLayer(
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
                        PropertyFactory.iconOffset(MapBoxConfig.ICON_OFFSET),
                        PropertyFactory.visibility(NONE)

                    )
            )

            //offlineMap()

            if(this.dialog != null) {
                this.dialog!!.dismiss()
            }
        }

        if(showVanLocationOnCreation) {
            showVanLocation(MapBoxConfig.MAX_ZOOM - 3, true)
        }

        //Create observer that updates the van position when position updates are received
        val vanObserver = Observer<VanEntity> { van ->
            if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                updateVanLocation(Point.fromLngLat(van.longitude, van.latitude), MapBoxConfig.MAX_ZOOM - 3)
            } else {
                updateVanLocationWithoutZoom(Point.fromLngLat(van.longitude, van!!.latitude))
            }
        }

        VanRepository.shared.getVanFlowById(VanAssistConfig.VAN_ID).observe(viewLifecycleOwner, vanObserver)
    }

/*
#################################################################################################################################
########################################### NAVIGATION UTILS ####################################################################
#################################################################################################################################
 */

    /* Created by Jasmin & Raluca
     * Function that starts the navigation for the current route. */
    private fun startTriggerNavigation() {

        val simulateRoute = false

         /* val options = NavigationLauncherOptions.builder()
            .directionsRoute(this.currentRoute)
            .shouldSimulateRoute(simulateRoute)
            .build()

        NavigationLauncher.startNavigation(this.activity, options)*/
    }


    /* Created by Jasmin
     * Function that creates the navigation between the origin and the destination point.
     * @param: origin: current location
     * @param: destination: the arriving point */
    private fun getRoute(origin_: Point, destination_: Point, profile: String) {

        /*try {
            val loc = this.mapBoxMap.locationComponent.lastKnownLocation as Location
            val bearing = loc.getBearing().toDouble()
            NavigationRoute.builder(this.requireContext())
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


                        this@MapFragmentOld.currentRoute = response.body()!!.routes()[0]

                        /* Draw route on the map */
                        if (this@MapFragmentOld.navigationMapRoute != null) {
                            this@MapFragmentOld.navigationMapRoute!!.removeRoute()
                        } else {
                            this@MapFragmentOld.navigationMapRoute =
                                NavigationMapRoute(null, this@MapFragmentOld.mapView, this@MapFragmentOld.mapBoxMap, R.style.NavigationMapRoute)
                        }
                        this@MapFragmentOld.navigationMapRoute!!.addRoute(this@MapFragmentOld.currentRoute)
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    }
                })
        } catch (e: SecurityException) {
            e.printStackTrace()
        }*/
    }


    private fun navigate(origin_: Point, destination_: Point, walking: Boolean) {
        try {
            var profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            if (walking) {
                profile = DirectionsCriteria.PROFILE_WALKING
            }
            //this.getRoute(origin_, destination_, profile)

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


/*
#################################################################################################################################
########################################### MAP DRAWING UTILS ###################################################################
#################################################################################################################################
 */

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
       /* val features = ArrayList<Feature>()
        this.parkingAreas = ParkingAreaRepository.shared.getAll()
        val nextAutoPaID = "parkingArea_429024483#3_0_12"
        if (!this.parkingAreas.isEmpty()) {
            for (pa in this.parkingAreas) {
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
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconImage(MapBoxConfig.MARKER_IMAGE),

                    /* Adjust the second number of the Float array based on the height of your marker image.
                       This is because the bottom of the marker should be anchored to the coordinate point, rather
                       than the middle of the marker being the anchor point on the map. */
                    PropertyFactory.iconOffset(MapBoxConfig.ICON_OFFSET)
                )
        )*/

        /* Search for best parking area, if no one is found -> use default */
        val nextDeliveryLocation = ParcelRepository.shared.getCurrentParcel()

        if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            this.nextParkingArea = ParkingAreaUtil.getPredefinedNextParkingArea(this.fSelectedParkingArea!!.name)
        } else {
            //this.nextParkingArea = ParkingAreaUtil.getNearestParkingArea(nextDeliveryLocation)
            val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())

            Log.i("MapFragment", "old next parking area: " + gNextParkingArea)

            this.nextParkingArea = MapFragmentOld.gNextParkingArea
            //api.getNextParkingArea()

        }


        if (this.nextParkingArea == null) {
            this.nextParkingArea = ParkingAreaRepository.shared.getParkingAreaById(ParkingAreaConfig.DEFAULT_PARKING_AREA)
        }

        Log.i("MapFragment", "Next parking area: " + this.nextParkingArea)

        /* set nextParkingArea as default destination */
        this.destination = Point.fromLngLat(this.nextParkingArea!!.long_.toDouble(), this.nextParkingArea!!.lat.toDouble())

        /* Create new camera position */
        newAnimatedCamPos(LatLng(this.nextParkingArea!!.lat.toDouble(), this.nextParkingArea!!.long_.toDouble()), MapBoxConfig.MAX_ZOOM - 3, 3000)

        val mutableList: MutableList<Feature> = arrayListOf()
        mutableList.add(Feature.fromGeometry(Point.fromLngLat(this.nextParkingArea!!.long_.toDouble(), this.nextParkingArea!!.lat.toDouble())))

        val source: GeoJsonSource = loadedMapStyle.getSourceAs(MapBoxConfig.MARKER_SOURCE_SELECTED)!!
        source.setGeoJson(FeatureCollection.fromFeatures(mutableList))

        /* add selected marker source */
        /*loadedMapStyle.addSource(
            GeoJsonSource(
                MapBoxConfig.MARKER_SOURCE_SELECTED,
                Feature.fromGeometry(Point.fromLngLat(this.nextParkingArea!!.long_.toDouble(), this.nextParkingArea!!.lat.toDouble()))
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
        )*/

    }

/*
#################################################################################################################################
########################################### APPLICATION CONTROL #################################################################
#################################################################################################################################
 */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Mapbox.getInstance(
            this.requireContext(),
            MapBoxConfig.MAP_BOX_ACCESS_TOKEN
        )
        val v = inflater.inflate(R.layout.fragment_map_old, container, false)


        if (testTargetApi() || checkPermissions()) {

            this.dialog = ProgressDialog.show(context, "", getString(R.string.loading_map___), true)


            this.mapView = v.findViewById(R.id.mapView)
            this.mapView.onCreate(savedInstanceState)

            this.mapView.getMapAsync(this)

            /*this.mapView.addOnDidFinishLoadingMapListener {
                Log.i("MapFragmentOld", "DidFinishListenerCalled!")
                this.dialog!!.dismiss()
            }*/

            this.currentParcel = ParcelRepository.shared.getCurrentParcel()

            val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())

            //TODO: CHANGE THIS TO ALWAYS USE THE LOCATION REQUESTED FROM THE SERVER
            /* Set Default Van Location only first time after simulation start */
            /* /> not when simulation is running and it is only resumed */
            /* /> would overwrite last position */
            if (SimulationConfig.isFirstVanLocationAfterSimulationStart) {
                VanRepository.shared.insert(
                    VanEntity(
                        VanAssistConfig.VAN_ID,
                        this.vehicleLocation.latitude(),
                        this.vehicleLocation.longitude(),
                        true,
                        "CLOSED",
                        "IN DELIVERY",
                        "OK",
                        ""
                    )
                )

                SimulationConfig.isFirstVanLocationAfterSimulationStart = false
            }



            /* declaring the animations */
            this.fabOpen = AnimationUtils.loadAnimation(context, R.anim.fab_open)
            this.fabClose = AnimationUtils.loadAnimation(context, R.anim.fab_close)
            this.fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            this.fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            this.fabRotateClockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_clockwise)
            this.fabRotateAnticlockwise = AnimationUtils.loadAnimation(context, R.anim.fab_rotate_anticlockwise)


            /* Config of bottom sheet aka parcel card */
            this.bottomSheetLinearLayout = v.bottom_sheet_v2
            this.bottomSheetBehavior = BottomSheetBehavior.from(this.bottomSheetLinearLayout!!)

            this.bottomSheetRecView = v.bottom_sheet_recView_panel
            this.bottomSheetSingleParcel = v.bottom_sheet_singleParcel_panel

            this.parcelAdapter = ParcelBottomSheetAdapter(this, this.bottomSheetBehavior!!)
            this.parcelAdapter.setState(0)
            this.parcelAdapter.setCourier(CourierRepository.shared.getCourier()!!)
                //this.setParcelInformation(activity as AppCompatActivity)


            this.recyclerView = v.bottom_sheet_recycler_view
            this.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this.context)
            this.recyclerView.setHasFixedSize(true)
            this.recyclerView.adapter = this.parcelAdapter

            this.bottomSheetRecView!!.visibility = View.GONE
            this.bottomSheetSingleParcel!!.visibility = View.VISIBLE

            this.bottomSheetStreetName = v.bottom_sheet_street_text_view_bSheet_v2 as TextView
            this.bottomSheetStreetNameAdditionalInformation =
                v.bottom_sheet_street_additional_information_text_view_bSheet_v2 as TextView
            this.bottomSheetRecipientName = v.bottom_sheet_recipient_name_text_view_bSheet_v2 as TextView
            this.bottomSheetRecipientNameAdditionalInformation =
                v.bottom_sheet_recipient_name__additional_information_text_view_bSheet_v2 as TextView
            this.bottomSheetPhoneButton = v.bottom_sheet_phone_button_bSheet_v2 as Button

            val courier = CourierRepository.shared.getCourier()

            if (courier?.darkMode!!) {
                this.bottomSheetStreetName!!.setTextColor(Color.WHITE)
                this.bottomSheetRecipientName!!.setTextColor(Color.WHITE)
            } else {
                this.bottomSheetStreetName!!.setTextColor(Color.BLACK)
                this.bottomSheetRecipientName!!.setTextColor(Color.BLACK)
            }

            this.bottomSheetPhoneButton!!.setOnClickListener {
                if (this.currentParcel?.phoneNumber != "" && this.currentParcel?.phoneNumber != null) {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:" + this.currentParcel?.phoneNumber)
                        startActivity(callIntent)
                    } catch (activityException: ActivityNotFoundException) {
                    }
                } else {
                    this.bottomSheetPhoneButton?.isEnabled = false
                }
            }

            val swipeButtonExpandedListener = object : SwipeButton.OnSwipeButtonListener {
                override fun OnSwipeButtonConfirm(v: View?) {
                    var currentParcel: ParcelEntity? = null
                    if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                        currentParcel = ParcelRepository.shared.getCurrentParcelForParkingArea(lastParkingArea!!.name)
                    } else {
                        //currentParcel = ParcelRepository.shared.getCurrentParcel()
                        currentParcel = ParcelRepository.shared.getCurrentParcelForParkingArea(lastParkingArea!!.name)
                    }
                    if (currentParcel != null) {
                        GamificationMode.run(context!!)
                        DynamicContent.reset()
                        SizeDependentWaiting.run(FragmentRepo.mapFragmentOld!!)
                        api.confirmParcelDeliverySuccess(currentParcel!!.id)
                    }
                }

                override fun OnSwipeButtonDecline(v: View?) {
                    var currentParcel: ParcelEntity? = null
                    if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                        currentParcel = ParcelRepository.shared.getCurrentParcelForParkingArea(lastParkingArea!!.name)
                    } else {
                        currentParcel = ParcelRepository.shared.getCurrentParcel()
                    }
                    if (currentParcel != null) {
                        GamificationMode.run(context!!)
                        DynamicContent.reset()
                        SizeDependentWaiting.run(FragmentRepo.mapFragmentOld!!)

                        api.confirmParcelDeliveryFailure(currentParcel!!.id)
                    }
                }

                override fun onSwipeButtonMoved(v: View) {
                    setDisabledCollapes(true)
                }

                override fun OnSwipeButtonFaded(v: View?) {
                    //bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                    FragmentRepo.mapFragmentOld!!.collapseBottomSheet()
                }
            }

            this.bottomSheetSwipeButton = v.swipe_btn_bSheet_v2 as SwipeButton
            this.bottomSheetSwipeButton!!.setSwipeListener(swipeButtonExpandedListener)

            /*bottomSheetPhoneButton!!.setOnClickListener {
                if (currentParcel?.phoneNumber != null) {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:" + currentParcel?.phoneNumber)
                        startActivity(callIntent)
                    } catch (activityException: ActivityNotFoundException) {
                    }
                }
            }*/

            //this.setParcelInformation(activity as AppCompatActivity)
            //this.updateAdapter()
            this.startGPSService()

            v.goto_launchpad.setOnClickListener { view ->
                //requireActivity().onBackPressed()
                (activity as MapActivity).startLaunchpadFragmentWithBackstack()
            }

            v.fab.setOnClickListener {

                if (this.isOpen) {
                    /* finish all open interactions */
                    this.finishSetNextParkingArea()
                    this.finishPedestrianRouting()
                    this.collapseFloatingActionButton(v)
                } else {
                    val courier = CourierRepository.shared.getCourier()
                    if (courier?.helpMode!!) {
                        v.textview_parkinglocation.startAnimation(this.fadeIn)
                        v.textview_vanlocation.startAnimation(this.fadeIn)
                        v.textview_deliverylocation.startAnimation(this.fadeIn)
                    }

                    v.fab_parkinglocation.startAnimation(this.fabOpen)
                    v.fab_vanlocation.startAnimation(this.fabOpen)
                    v.fab_deliverylocation.startAnimation(this.fabOpen)
                    v.fab.startAnimation(this.fabRotateClockwise)

                    v.fab_deliverylocation.isClickable = true
                    v.fab_vanlocation.isClickable = true
                    v.fab_parkinglocation.isClickable = true

                    this.isOpen = true
                }
            }

            /* Parcel Card */
            //this.bottomSheetLinearLayout = v.bottom_sheet
            /*this.bottomSheetLinearLayout = v.bottom_sheet_v2
            this.bottomSheetBehavior = BottomSheetBehavior.from(this.bottomSheetLinearLayout!!)*/

            /* used for swiping button (do not collapse parcel card while button swipes) */
            //var disabledCollapse = false

            v.topPanel_bShet_v2.setOnTouchListener { _, _ ->
                disabledCollapse = false
                false
            }
            /*v.bottom_sheet_pull_btn_v2.setOnTouchListener {_, _ ->
                MapFragmentOld.disabledCollapse = false
                false
            }*/

            /* Set swipe listener to SwipeButton */
           /* val swipeButtonExpandedListener = object : SwipeButton.OnSwipeButtonListener {
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
                    this@MapFragmentOld.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            val swipeButton = v.swipe_btn_v2 as SwipeButton
            swipeButton.setSwipeListener(swipeButtonExpandedListener)*/

            /* Set on touch listener for Pull Line Button */
            v.bottom_sheet_pull_btn_bSheet_v2.setOnClickListener {
                if (this.bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
                }
                if (this.bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
                    this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            bottomSheetBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    v.fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset)
                        .setDuration(0).start()
                    if(bottomSheetState == 0) {
                        v.goto_launchpad.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset)
                            .setDuration(0).start()
                    }

                    if (this@MapFragmentOld.isOpen) {
                        collapseFloatingActionButton(v)
                    }
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    v.fab.isClickable = newState != BottomSheetBehavior.STATE_EXPANDED
                    if(bottomSheetState == 0) {
                        v.goto_launchpad.isClickable = newState != BottomSheetBehavior.STATE_EXPANDED
                    }
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
            showVanLocation(MapBoxConfig.MAX_ZOOM - 3, true)
        }

        v.fab_deliverylocation.setOnClickListener {
            showNextDeliveryLocation(MapBoxConfig.MAX_ZOOM - 3, true)
        }

        v.fab_parkinglocation.setOnClickListener {

            if (ParcelRepository.shared.getCurrentParcel() != null) {
                /*if (this.wasclicked && !this.routeShown) {

                    /* Second Interaction Step */
                    showDialogOK(getString(R.string.select_next_parking_area),
                        DialogInterface.OnClickListener { _, which ->

                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    /* strip layers and sources */
                                    this.mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER)
                                    this.mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE)
                                    fab_parkinglocation.setImageResource(R.drawable.ic_navigation_grey_24dp)
                                    this.routeShown = true
                                    this.navigate(this.vehicleLocation, this.destination, false)
                                }

                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                        })
                }*/ if (!this.wasclicked) {
                    /* First Interaction Step */
                    this.finishSetNextParkingArea()
                    this.finishGetVanLocation()
                    this.finishPedestrianRouting()

                    this.wasclicked = true
                    //this.mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)
                    //this.mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE_SELECTED)

                    Toast.createToast(getString(R.string.parking_area_confirmation))

                    /* set Custom vehicle marker */
                    /*val icon = IconFactory.getInstance(requireActivity())
                    /* Add the marker to the map */
                    this.mapBoxMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()))
                            .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))
                    )*/
                    this.showVanLocation(MapBoxConfig.MAX_ZOOM - 3, false)

                   /* this.mapBoxMap.getStyle {

                        /* Add Marker Image */
                        it.addImage(
                            MapBoxConfig.MARKER_IMAGE,
                            getBitmapFromVectorDrawable(this.requireContext(), R.drawable.alpha_p_circle)
                        )

                        /* Add Selected Marker Image */
                        it.addImage(
                            MapBoxConfig.MARKER_IMAGE_SELECTED,
                            BitmapFactory.decodeResource(this.resources, R.drawable.ic_custom_parker_pin_red)
                        )

                        addMarkers(it)
                    }*/

                    this.mapBoxMap.style!!.getLayer(MapBoxConfig.MARKER_STYLE_LAYER)!!.setProperties(visibility(VISIBLE))
                    this.mapBoxMap.style!!.getLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)!!.setProperties(visibility(VISIBLE))

                    addMarkers(this.mapBoxMap.style!!)
                    fab_parkinglocation.setImageResource(R.drawable.ic_custom_parker_confirm)
                }
                //if (this.wasclicked && this.routeShown) {
                else if(this.wasclicked) {
                    /* Last Interaction Step */
                    this.postNextParkingAreaToServer()
                    /* finish the Interaction and set back to initial */
                    this.finishSetNextParkingArea()

                    //if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                        this.changeBottomSheet(1)
                    //}
                }
            } else {
                Toast.createToast(getString(R.string.error_no_parcel_available))
            }
        }

        this.changeBottomSheet(this.bottomSheetState)
        this.resetParkingAreas()

        return v
    }

    override fun onResume() {
        super.onResume()
        if (this.broadcastReceiver == null) {
            this.broadcastReceiver = object : BroadcastReceiver() {
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
                            var nextParkingArea: ParkingAreaEntity? = ParkingAreaUtil.getNearestParkingArea(nextDeliveryLocation)
                            if (nextParkingArea == null) {
                                nextParkingArea =
                                    ParkingAreaRepository.shared.getParkingAreaById("parkingArea_-24828111#0_0_15")
                            }
                            postNextParkingAreaToServer(nextParkingArea!!)
                            IntelligentDriving.reset()
                        }
                    }
                }
            }
        }
        activity?.registerReceiver(this.broadcastReceiver, IntentFilter("location_update"))
    }

    override fun onDestroy() {
        super.onDestroy()

        if (this.broadcastReceiver != null) {
            requireActivity().unregisterReceiver(this.broadcastReceiver)
        }

        if (gpsService != null) {
            requireActivity().stopService(gpsService)
        }

        if(this.dialog != null) {
            this.dialog!!.dismiss()
        }
    }

    /* PERMISSIONS */
    @TargetApi(VanAssistConfig.TARGET_API)
    fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeeded = ArrayList<String>()
        for (p in this.permissions) {
            result = requireContext().checkSelfPermission(p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
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


/*
#################################################################################################################################
########################################### ON MAP CLICK LISTENER ###############################################################
#################################################################################################################################
 */

    /* EventHandler for interaction with the map */
    override fun onMapClick(point: LatLng): Boolean {
        if (this.wasclicked) {
            val style = this.mapBoxMap.style
            if (style != null) {

                val selectedMarkerSymbolLayer = style.getLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED) as SymbolLayer

                val pixel = this.mapBoxMap.projection.toScreenLocation(point)
                val features = this.mapBoxMap.queryRenderedFeatures(pixel, MapBoxConfig.MARKER_STYLE_LAYER)
                val selectedFeature = this.mapBoxMap.queryRenderedFeatures(
                    pixel, MapBoxConfig.MARKER_STYLE_LAYER_SELECTED
                )

                if (selectedFeature.size > 0 && this.markerSelected) {
                    return false
                }

                if (features.isEmpty()) {
                    if (this.markerSelected) {
                        deselectMarker(selectedMarkerSymbolLayer)
                    }
                    return false
                }

                val mutableList: MutableList<Feature> = arrayListOf()
                mutableList.add(Feature.fromGeometry(features[0].geometry()))

                val geoCoordinateJSON = JSONObject(features[0].geometry()!!.toJson())
                val geoCoordinate = geoCoordinateJSON.getJSONArray("coordinates")

                this.selectedParkingArea = features[0]

                val longitude = geoCoordinate.getDouble(0)
                val latitude = geoCoordinate.getDouble(1)
                this.destination = Point.fromLngLat(longitude, latitude)
                newAnimatedCamPos(LatLng(latitude, longitude), this.mapBoxMap.maxZoomLevel - 3, MapBoxConfig.CAM_POS_ANIMATION_IN_MS)

                val source: GeoJsonSource = style.getSourceAs(MapBoxConfig.MARKER_SOURCE_SELECTED)!!
                source.setGeoJson(FeatureCollection.fromFeatures(mutableList))

                if (this.markerSelected) {
                    deselectMarker(selectedMarkerSymbolLayer)
                }
                if (features.size > 0) {
                    selectMarker(selectedMarkerSymbolLayer)
                }
            }
        }
        return true
    }


/*
#################################################################################################################################
########################################### GENERAL UI UTILS ####################################################################
#################################################################################################################################
 */


    /* Manages the display of the van location in the map */
    fun showVanLocation(zoom: Double, animation: Boolean) {
        //val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())
        //api.getCurrentVanState()

        var van = VanRepository.shared.getVanById(VanAssistConfig.VAN_ID)!!
        this.vehicleLocation = Point.fromLngLat(van.longitude, van.latitude)
        Log.i("MapFragmentOld", "Current vehicle position: " + van.latitude + " " + van.longitude)

        val icon = IconFactory.getInstance(this.requireActivity())
        this.wasClickedVanLocation = true
        this.mapBoxMap.addMarker(
            MarkerOptions()
                .position(LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()))
                .title("DPD Van")
                .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))

        )
        if (animation) {
            newAnimatedCamPos(
                LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()),
                zoom,
                VanAssistConfig.VAN_ARRIVAL_ZOOM_DURATION
            )
        } else {
            newCamPos(LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()), zoom)
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

        if (!this.wasClickedDeliveryLocation) {
            this.finishSetNextParkingArea()
            this.finishPedestrianRouting()

            val icon = IconFactory.getInstance(this.requireActivity())

            this.wasClickedDeliveryLocation = true

            if(this.wasClickedVanLocation){
                this.mapBoxMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()))
                        .title("DPD Van")
                        .icon(icon.fromResource(R.mipmap.ic_custom_dpd_van_cropped))

                )
            }

            this.mapBoxMap.addMarker(
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

        } else if (this.wasClickedDeliveryLocation && !this.deliveryRouteShown) {

            /* confirm routing to delivery location from GPS AKA Some static location for now */
            try {
                val employeePosition = Point.fromLngLat(
                    this.mapBoxMap.locationComponent.lastKnownLocation!!.longitude,
                    this.mapBoxMap.locationComponent.lastKnownLocation!!.latitude
                )
                val delLocation = Point.fromLngLat(longitude, latitude)
                this.navigate(employeePosition, delLocation, true)
                this.deliveryRouteShown = true
                fab_deliverylocation.setImageResource(R.drawable.ic_custom_parker_confirm)

            } catch (e: SecurityException) {
                e.printStackTrace()
            }

        } else if (this.wasClickedDeliveryLocation && this.deliveryRouteShown) {
            this.startTriggerNavigation()
        }
    }


    /* Finishes the pedestrian routing */
    private fun finishPedestrianRouting() {
        fab_deliverylocation.setImageResource(R.drawable.ic_inbox_black_24dp)
        this.wasClickedDeliveryLocation = false
        this.deliveryRouteShown = false

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

        this.mapBoxMap.easeCamera(
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
        for (marker in this.mapBoxMap.markers) {
            if (marker.title == MapBoxConfig.MARKER_TITLE_VAN_IS_DRIVING) {
                this.mapBoxMap.removeMarker(marker)
            }
        }
    }


    /* Shows the target parking position on the map during the navigation of the van */
    fun addParkingLocationWhenVanStartDriving() {
        val icon = IconFactory.getInstance(this.requireActivity())
        var markerIsSet = false
        for (marker in this.mapBoxMap.markers) {
            if (marker.title == MapBoxConfig.MARKER_TITLE_DESTINATION) {
                markerIsSet = true
            }
        }

        if (markerIsSet == false) {
            this.mapBoxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(this.destination.latitude(), this.destination.longitude()))
                    .title(MapBoxConfig.MARKER_TITLE_DESTINATION)
                    .icon(icon.fromBitmap(getBitmapFromVectorDrawable(this.requireContext(), R.drawable.alpha_p_circle)))
            )
        }
    }


    /* Removes the target parkingArea after the van has arrived in the parkingArea */
    fun removeParkingLocationWhenVanHasParked() {
        for (marker in this.mapBoxMap.markers)
            if (marker.title == MapBoxConfig.MARKER_TITLE_DESTINATION) {
                this.mapBoxMap.removeMarker(marker)
            }
    }


    /* Updates the van location without zooming to the van */
    fun updateVanLocationWithoutZoom(point: Point) {
        this.vehicleLocation = point
        val icon = IconFactory.getInstance(this.requireActivity())
        if (this.currentVanPosition != null) {
            this.currentVanPosition!!.remove()
        }
        this.removeDrivingPositionOfDPDVan()
        this.mapBoxMap.addMarker(
            MarkerOptions()
                .position(LatLng(this.vehicleLocation.latitude(), this.vehicleLocation.longitude()))
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
        this.mapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun getLastKnownLocation(): LatLng {
        return LatLng(this.mapBoxMap.locationComponent!!.lastKnownLocation!!.latitude, this.mapBoxMap.locationComponent!!.lastKnownLocation!!.longitude)
    }


    private fun newCamPos(cameraPosition: CameraPosition) {
        this.mapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }


    fun hideBottomSheetFromOutSide(event: MotionEvent) {
        if (this.bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            val outRect = Rect()
            this.bottomSheetLinearLayout!!.getGlobalVisibleRect(outRect)
            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt()))
                this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }


    /* Collapes FloatingActionButton e.g. after Parcel confirmation */
    private fun collapseFloatingActionButton(v: View) {
        val courier = CourierRepository.shared.getCourier()
        if (courier?.helpMode!!) {
            v.textview_parkinglocation.startAnimation(this.fadeOut)
            v.textview_vanlocation.startAnimation(this.fadeOut)
            v.textview_deliverylocation.startAnimation(this.fadeOut)
        }

        v.fab_parkinglocation.startAnimation(this.fabClose)
        v.fab_vanlocation.startAnimation(this.fabClose)
        v.fab_deliverylocation.startAnimation(this.fabClose)
        v.fab.startAnimation(this.fabRotateAnticlockwise)

        v.fab_deliverylocation.isClickable = false
        v.fab_vanlocation.isClickable = false
        v.fab_parkinglocation.isClickable = false

        this.isOpen = false
    }

    private fun collapseHomeButton(v: View) {
        v.goto_launchpad.startAnimation(this.fabClose)
        v.goto_launchpad.isClickable = false
    }

    /* Sets parcel information in the bottom sheet */
    fun setParcelInformation(con:AppCompatActivity) {
        if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            if(this.lastParkingArea == null) {
                this.lastParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H1")
            }
            this.currentParcel = ParcelRepository.shared.getCurrentParcelForParkingArea(this.lastParkingArea!!.name)
        } else {
            //this.currentParcel = ParcelRepository.shared.getCurrentParcel()
            if(this.lastParkingArea == null) {
                this.lastParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H1")
            }
            Log.i("MapFragment", this.lastParkingArea.toString())
            this.currentParcel = ParcelRepository.shared.getCurrentParcelForParkingArea(this.lastParkingArea!!.name)
        }

        if(this.currentParcel == null) {
            this.changeBottomSheet(0)
            if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
                VanRepository.shared.updateVanLocationById(
                    VanAssistConfig.VAN_ID,
                    this.fSelectedParkingArea!!.lat.toDouble(),
                    this.fSelectedParkingArea!!.long_.toDouble()
                )
            }
        }

        if(this.bottomSheetState == 0) {
            var parcelList: ArrayList<ParcelEntity>? = null

            if(this.fSelectedParkingArea == null) {
                this.fSelectedParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H1")
            }

            parcelList = ArrayList<ParcelEntity>(ParcelRepository.shared.getParcelsByParkingAreaName(this.fSelectedParkingArea!!.name).size)
            parcelList.addAll(ParcelRepository.shared.getParcelsByParkingAreaName(this.fSelectedParkingArea!!.name))
            this.parcelAdapter.setState(0)

            this.parcelAdapter.setParcels(parcelList!!)
            this.recyclerView.adapter!!.notifyDataSetChanged()
        } else {
            if (this.currentParcel?.address == null) {
                this.bottomSheetStreetName?.text = getString(R.string.no_data_available)
            } else {
                this.bottomSheetStreetName?.text = this.currentParcel?.address
            }

            if (this.currentParcel?.additionalAddressInformation == null) {
                this.bottomSheetStreetNameAdditionalInformation?.text = con.getString(R.string.no_data_available)
            } else {
                this.bottomSheetStreetNameAdditionalInformation?.text = this.currentParcel?.additionalAddressInformation
            }


            if (this.currentParcel?.nameOfRecipient == null) {
                this.bottomSheetRecipientName?.text = getString(R.string.no_data_available)
            } else {
                this.bottomSheetRecipientName?.text = this.currentParcel?.nameOfRecipient
            }


            if (this.currentParcel?.additionalRecipientInformation == null) {
                this.bottomSheetRecipientNameAdditionalInformation?.text = con.getString(R.string.no_data_available)
            } else {
                this.bottomSheetRecipientNameAdditionalInformation?.text = this.currentParcel?.additionalRecipientInformation
            }

            if (this.currentParcel?.phoneNumber != null) {
                this.bottomSheetPhoneButton?.isEnabled = true
            } else {
                this.bottomSheetPhoneButton?.isEnabled = false
            }
        }
    }

    fun updateAdapter() {
        if(this.bottomSheetState == 0) {
            var parcelList: List<ParcelEntity>? = null
            if(this.fSelectedParkingArea == null) {
                this.resetParkingAreas()
            }
            parcelList = ArrayList<ParcelEntity>(ParcelRepository.shared.getParcelsByParkingAreaName(this.fSelectedParkingArea!!.name).size)
            parcelList.addAll(ParcelRepository.shared.getParcelsByParkingAreaName(this.fSelectedParkingArea!!.name))
            this.parcelAdapter.setState(0)
            this.parcelAdapter.setParcels(parcelList!!)
            this.recyclerView.adapter!!.notifyDataSetChanged()
        }
    }


    /* Startes GPS as a service */
    private fun startGPSService() {
        val gpsService = Intent(requireContext().applicationContext, GPSService::class.java)
        requireActivity().startService(gpsService)
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
        val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())
        /* send over parkingArea retrieved from ID from Repo */
        if (this.selectedParkingArea != null) {
            api.postNextParkingLocation(this.selectedParkingArea!!.getStringProperty("PA ID"))

            this.lastParkingArea = ParkingAreaRepository.shared.getParkingAreaById(this.fSelectedParkingArea!!.id)

            this.fSelectedParkingArea = ParkingAreaRepository.shared.getParkingAreaById(this.selectedParkingArea!!.getStringProperty("PA ID"))
            this.selectedParkingArea = null
        } else {
            this.lastParkingArea = ParkingAreaRepository.shared.getParkingAreaById(this.fSelectedParkingArea!!.id)

            this.fSelectedParkingArea = ParkingAreaRepository.shared.getParkingAreaById(this.nextParkingArea!!.id)
            api.postNextParkingLocation(this.nextParkingArea!!.id)
        }
    }

    /* Send next parkingArea to the server */
    fun postNextParkingAreaToServer(parkingArea: ParkingAreaEntity) {
        val api = VanAssistAPIController(requireActivity() as AppCompatActivity, requireContext())
        api.postNextParkingLocation(parkingArea.id)
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
        this.markerSelected = false
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
        this.markerSelected = true

    }

    /* Finish the interaction with the map (get van) */
    private fun finishGetVanLocation() {

        this.wasClickedVanLocation = false
        /* Strip markers */
        for (m in this.mapBoxMap.markers) {
            this.mapBoxMap.removeMarker(m)
        }
        //newCamPos(this.originalCamPos)
        if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            newAnimatedCamPos(LatLng(this.lastParkingArea!!.lat.toDouble(), this.lastParkingArea!!.long_.toDouble()), MapBoxConfig.MAX_ZOOM - 3, 3000)
        }else {
            newAnimatedCamPos(this.getLastKnownLocation(), MapBoxConfig.MAX_ZOOM - 3, 3000)
        }
    }

    /* Finish the interaction with the map (set parkingArea) */
    private fun finishSetNextParkingArea() {

        /* reset wasClicked */
        if (this.wasclicked) {
            this.wasclicked = false
        }

        /* reset markerSelected */
        if (this.markerSelected) {
            this.markerSelected = false
        }

        /* reset the routes */
        if (this.navigationMapRoute != null) {
            //this.navigationMapRoute!!.removeRoute()
            this.routeShown = false
        }

        /* strip all markers */
        for (m in this.mapBoxMap.markers) {
            this.mapBoxMap.removeMarker(m)
        }

        /* strip all layers and sources */
        val style = this.mapBoxMap.style
       /* if (style != null) {
            /* strip layers and sources */
            this.mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER)
            this.mapBoxMap.style?.removeLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)
            this.mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE)
            this.mapBoxMap.style?.removeSource(MapBoxConfig.MARKER_SOURCE_SELECTED)
            fab_parkinglocation.setImageResource(R.drawable.ic_local_parking_black_24dp)
        }*/

        style!!.getLayer(MapBoxConfig.MARKER_STYLE_LAYER)!!.setProperties(visibility(NONE))
        style!!.getLayer(MapBoxConfig.MARKER_STYLE_LAYER_SELECTED)!!.setProperties(visibility(NONE))
        fab_parkinglocation.setImageResource(R.drawable.ic_local_parking_black_24dp)

        //newCamPos(this.originalCamPos)
        if(VanAssistConfig.USE_DEMO_SCENARIO_DATA) {
            newAnimatedCamPos(LatLng(this.lastParkingArea!!.lat.toDouble(), this.lastParkingArea!!.long_.toDouble()), MapBoxConfig.MAX_ZOOM - 3, 3000)
        }else {
            newAnimatedCamPos(this.getLastKnownLocation(), MapBoxConfig.MAX_ZOOM - 3, 3000)
        }
    }

    /* Created by Jasmin & Raluca
     * Function that includes offline navigation through the map */
    private fun offlineMap() {

        val offlineManager = OfflineManager.getInstance(this.requireActivity())

        /* Create bounding box for offline region */
        val latLngBounds = LatLngBounds.Builder()
            .include(MapBoxConfig.OFFLINE_MAP_BOUND_NORTH_WEST)
            .include(MapBoxConfig.OFFLINE_MAP_BOUND_SOUTH_EAST)
            .build()

        /* Define offline region */
        val definition = OfflineTilePyramidRegionDefinition(
            this.mapBoxMap.style?.url,
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
        val notificationOptions = NotificationOptions.builder(this.requireActivity())
            .smallIconRes(R.drawable.mapbox_logo_icon)
            .returnActivity(this.activity!!::class.java.name)
            .build()

        if (!regionDownloaded) {
            /* Start downloading the map tiles for offline use */
            OfflinePlugin.getInstance(this.requireActivity()).startDownload(
                OfflineDownloadOptions.builder()
                    .definition(definition)
                    .metadata(OfflineUtils.convertRegionName("Neuenheim"))
                    .notificationOptions(notificationOptions)
                    .build()
            )
        }
    }

    fun expandBottomSheet() {
        this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setShowVanLocationOnCreation() {
        showVanLocationOnCreation = true
    }

    fun collapseBottomSheet() {
        this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun setDisabledCollapes(disabledCollapse: Boolean) {
        this.disabledCollapse = disabledCollapse
        this.bottomSheetBehavior!!.isDraggable = !disabledCollapse
    }

    fun changeBottomSheet(state: Int) {
        this.bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

        this.bottomSheetState = state

        if(state == 0) {
            this.updateAdapter()
            this.bottomSheetSingleParcel!!.visibility = View.GONE
            this.bottomSheetRecView!!.visibility = View.VISIBLE
        } else {
            this.setParcelInformation(activity as AppCompatActivity)
            this.bottomSheetSingleParcel!!.visibility = View.VISIBLE

            this.bottomSheetSwipeButton!!.removeAllViews()
            this.bottomSheetSwipeButton!!.init(requireContext())

            this.bottomSheetRecView!!.visibility = View.GONE
        }
    }

    fun resetParkingAreas() {
        this.lastParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H1")
        this.nextParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H2")
        this.fSelectedParkingArea = ParkingAreaRepository.shared.getParkingAreaByName("H1")
        MapFragmentOld.gNextParkingArea = null
        VanRepository.shared.updateVanLocationById(VanAssistConfig.VAN_ID, this.lastParkingArea!!.lat.toDouble(), this.lastParkingArea!!.long_.toDouble())
    }
}
