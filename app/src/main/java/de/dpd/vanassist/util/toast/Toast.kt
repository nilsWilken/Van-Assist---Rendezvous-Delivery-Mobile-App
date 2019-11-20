package de.dpd.vanassist.util.toast

import android.widget.Toast
import de.dpd.vanassist.config.FragmentTag
import de.dpd.vanassist.util.FragmentRepo

class Toast {

    companion object {

        fun createToast(message:String) {

            if(FragmentRepo.mapActivity != null) {
                val parcelListFragment = FragmentRepo.mapActivity!!.supportFragmentManager.findFragmentByTag(FragmentTag.OPEN_DELIVERY)
                val mapFragment = FragmentRepo.mapActivity!!.supportFragmentManager.findFragmentByTag(FragmentTag.MAP)
                val settingsFragment = FragmentRepo.mapActivity!!.supportFragmentManager.findFragmentByTag(FragmentTag.SETTINGS)
                val launchpadFragment = FragmentRepo.mapActivity!!.supportFragmentManager.findFragmentByTag(FragmentTag.LAUNCHPAD)

                if(mapFragment != null && mapFragment.isVisible) {
                    mapFragment.activity!!.runOnUiThread {
                        Toast.makeText(mapFragment.context, message, Toast.LENGTH_SHORT).show()
                    }
                } else if(parcelListFragment != null && parcelListFragment.isVisible) {
                    parcelListFragment.activity!!.runOnUiThread {
                        Toast.makeText(parcelListFragment.context, message, Toast.LENGTH_SHORT).show()
                    }
                } else if(settingsFragment != null && settingsFragment.isVisible) {
                    settingsFragment.activity!!.runOnUiThread {
                        Toast.makeText(settingsFragment.context, message, Toast.LENGTH_SHORT).show()
                    }
                } else if(launchpadFragment != null && launchpadFragment.isVisible) {
                    launchpadFragment.activity!!.runOnUiThread {
                        Toast.makeText(launchpadFragment.context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}