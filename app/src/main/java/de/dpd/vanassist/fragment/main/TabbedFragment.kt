package de.dpd.vanassist.fragment.main

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.fragment_tabbed.view.*
import com.google.android.material.tabs.TabLayout
import android.view.animation.Animation
import android.view.animation.AccelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.DecelerateInterpolator
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.parcel.ParcelState

/* A simple [Fragment] subclass.*/
class TabbedFragment : Fragment() {

    private lateinit var parcelAdapter: PagerAdapter
    private lateinit var viewPager: ViewPager

    private lateinit var delivered : ParcelListFragment
    private lateinit var notDelivered : ParcelListFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_tabbed, container, false)

        /* initialize parcel lists */
        this.delivered = ParcelListFragment.newInstance(ParcelState.DELIVERY_SUCCESS)
        this.notDelivered = ParcelListFragment.newInstance(ParcelState.DELIVERY_FAILURE)

        FragmentRepo.parcelListFragmentDeliverySuccess = this.delivered
        FragmentRepo.parcelListFragmentDeliveryFailure = this.notDelivered

        v.pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(v.tab_layout))

        v.tab_layout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            @SuppressLint("RestrictedApi")
            override fun onTabSelected(tab: TabLayout.Tab) {

                val zeroFab = delivered.fab
                val oneFab = notDelivered.fab
                if (zeroFab != null && oneFab != null) {
                    if (tab.position == 0) {
                        oneFab.visibility = View.GONE
                        zeroFab.visibility = View.VISIBLE
                        animateFab(zeroFab)
                    } else {
                        zeroFab.visibility = View.GONE
                        oneFab.visibility = View.VISIBLE
                        animateFab(oneFab)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        this.parcelAdapter = PagerAdapter(fragmentManager!!)
        this.viewPager = v.pager
        this.viewPager.adapter = this.parcelAdapter

        FragmentRepo.tabbedFragment = this

        return v
    }


    /* Helper class for managing TabbedView */
    inner class PagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        private val tabTitles = arrayOf(getString(R.string.delivered), getString(R.string.not_delivered))

        override fun getPageTitle(position: Int): CharSequence? {
            return tabTitles[position]
        }

        override fun getItem(position: Int): Fragment? {

            return when (position) {
                0 -> delivered
                1 -> notDelivered
                else -> null
            }
        }

        override fun getCount(): Int {
            return 2
        }
    }


    /* Method for animating the FAB on tab switch */
    private fun animateFab(fab : FloatingActionButton) {
        fab.clearAnimation()
        /* Scale down animation */
        val shrink = ScaleAnimation(1f, 0.2f, 1f, 0.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        /* animation duration in milliseconds */
        shrink.duration = 150
        shrink.interpolator = DecelerateInterpolator()
        shrink.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                /* Scale up animation */
                val expand = ScaleAnimation(
                    0.2f,
                    1f,
                    0.2f,
                    1f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
                )
                /* animation duration in milliseconds */
                expand.duration = 100
                expand.interpolator = AccelerateInterpolator()
                fab.startAnimation(expand)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        fab.startAnimation(shrink)
    }
}
