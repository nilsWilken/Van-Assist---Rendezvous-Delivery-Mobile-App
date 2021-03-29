package de.dpd.vanassist.util

import de.dpd.vanassist.activity.MapActivity
import de.dpd.vanassist.fragment.main.*
import de.dpd.vanassist.fragment.main.launchpad.LaunchpadFragment
import de.dpd.vanassist.fragment.main.launchpad.ParcelListFragment
import de.dpd.vanassist.fragment.main.map.MapFragmentOld

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