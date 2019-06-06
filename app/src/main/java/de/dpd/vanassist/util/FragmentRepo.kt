package de.dpd.vanassist.util

import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.fragment.main.LaunchpadFragment
import de.dpd.vanassist.fragment.main.MapFragment
import de.dpd.vanassist.fragment.main.ParcelListFragment
import de.dpd.vanassist.fragment.main.TabbedFragment

class FragmentRepo {

    companion object {
        var mapActivity: MapActivity? = null
        var mapFragment: MapFragment? = null
        var launchPadFragment: LaunchpadFragment? = null
        var parcelListFragment: ParcelListFragment? = null
        var tabbedFragment: TabbedFragment? = null
        var parcelListFragmentDeliverySuccess: ParcelListFragment? = null
        var parcelListFragmentDeliveryFailure: ParcelListFragment? = null


    }
}