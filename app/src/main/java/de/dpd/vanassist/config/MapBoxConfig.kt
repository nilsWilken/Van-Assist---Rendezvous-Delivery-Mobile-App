package de.dpd.vanassist.config

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

class MapBoxConfig {

    companion object {

        const val MAP_BOX_ACCESS_TOKEN = "pk.eyJ1IjoidmFuYXNzaXN0IiwiYSI6ImNqdTl2eDM0czJjaHY0M2xsYzg1bjFtMmYifQ.YkAR1cSNtoEbI_SSNKcrlg"

        const val MAP_BOX_LIGHT_STYLE = "mapbox://styles/vanassist/cju9ygw3o1pzi1fml5x9kcbdw"
        const val MAP_BOX_DARK_STYLE = "mapbox://styles/vanassist/cjv68g5pc0o7v1fqqqq86cma8"

        val OFFLINE_MAP_BOUND_NORTH_WEST = LatLng(49.4291, 8.6598)
        val OFFLINE_MAP_BOUND_SOUTH_EAST = LatLng(49.4037, 8.7148)

        private val BOUND_CORNER_SE = LatLng(49.4037, 8.7148)

        const val MIN_ZOOM = 8.5
        const val MAX_ZOOM = 18.5

        const val MULTIPLE_PERMISSIONS = 10

        const val SET_MARKER_DURATION_IN_MS = 500
        const val CAM_POS_ANIMATION_IN_MS = 500
        const val MARKER_ANIMATION_DURATION = 300L

        const val DEFAULT_INTERVAL_IN_MS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MS * 5

        val DEFAULT_VEHICLE_LOCATION = Point.fromLngLat(8.678421, 49.416937)!!

        const val NAVIGATION_TOLERANCE = 90.0

        val ICON_OFFSET = floatArrayOf(0f, -8f).toTypedArray()

        const val MARKER_PROPERTY_ICON_OPACITY = 0.9f

        val MARKER_PROPERTY_TEXT_OFFSET = floatArrayOf(0f, 0.8f).toTypedArray()

        const val MARKER_PROPERTY_TEXT_SIZE = 11f


        /* ParkingAreaEntity Sign Layer */
        const val MARKER_SOURCE = "markers-source"
        const val MARKER_STYLE_LAYER = "markers-style-layer"
        const val MARKER_IMAGE = "custom-marker"

        /* Selected Marker Layer */
        const val MARKER_SOURCE_SELECTED = "markers-source-selected"
        const val MARKER_STYLE_LAYER_SELECTED = "markers-style-layer-selected"
        const val MARKER_IMAGE_SELECTED = "custom-marker-selected"

        const val MARKER_TITLE_VAN_IS_DRIVING = "DPD VanEntity Driving"
        const val MARKER_TITLE_DESTINATION = "destination"
    }

}