package de.dpd.vanassist.util

import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.fragment.main.*

class FragmentRepo {

    companion object {
        var mapActivity: MapActivity? = null
        var mapFragmentOld: MapFragmentOld? = null
        var launchPadFragment: LaunchpadFragment? = null
        var parcelListFragment: ParcelListFragment? = null
        var tabbedFragment: TabbedFragment? = null
        var parcelListFragmentDeliverySuccess: ParcelListFragment? = null
        var parcelListFragmentDeliveryFailure: ParcelListFragment? = null


    }
}