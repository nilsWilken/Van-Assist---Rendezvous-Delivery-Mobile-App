package de.dpd.vanassist.fragment.main

import de.dpd.vanassist.R
import de.dpd.vanassist.adapters.ParcelInformationAdapter
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.controls.DragCallback
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.util.parcel.ParcelState

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import android.util.Log
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.database.repository.CourierRepository
import de.dpd.vanassist.util.FragmentRepo

import kotlinx.android.synthetic.main.fragment_parcel_list.view.*
import java.util.*
import kotlin.collections.ArrayList


/* Fragment to hold a list of parcels */
@SuppressLint("ValidFragment")
class ParcelListFragment: androidx.fragment.app.Fragment() {

    var fab : FloatingActionButton? = null
    private var targetState: Int = 0
    lateinit var parcelAdapter:ParcelInformationAdapter
    lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView


    companion object {
        fun newInstance(state: Int): ParcelListFragment {
            val args = Bundle()
            args.putInt("state", state)
            val fragment = ParcelListFragment()
            fragment.arguments = args
            return fragment
        }

        var parcelList: ArrayList<ParcelEntity> = arrayListOf()

    }


    init {
        val args = arguments
        if (args != null) {
            val state = args.getInt("state", 0)
            this.targetState = state
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_parcel_list, container, false)
        val args = arguments
        if (args != null) {
            val state = args.getInt("state", 0)
            this.targetState = state
        }

        fab = v.goto_launchpad_from_delivered as FloatingActionButton
        v.goto_launchpad_from_delivered.setOnClickListener {
            activity?.onBackPressed()
        }

        /* adding recycler view to fragment */
        this.recyclerView = v.deliveryparcel_recyclerview
        this.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this.context)
        this.recyclerView.setHasFixedSize(true)

        this.parcelAdapter = ParcelInformationAdapter(this)
        this.parcelAdapter.setState(targetState)
        this.parcelAdapter.setParcels(getParcelList(targetState)!!)
        this.parcelAdapter.setCourier(CourierRepository.shared.getCourier()!!)

        this.recyclerView.adapter = this.parcelAdapter

        /* add ItemTouchHelper */
        val dragCallback = DragCallback(this.parcelAdapter)
        val touchHelper = ItemTouchHelper(dragCallback)
        this.parcelAdapter.setTouchHelper(touchHelper)
        touchHelper.attachToRecyclerView(this.recyclerView)

        FragmentRepo.parcelListFragment = this

        return v
    }


    fun changeParcelOrder(parcelId:String, newPosition:Int) {

        VanAssistAPIController(activity as AppCompatActivity).updateParcelPosition(parcelId, newPosition)
    }


    fun updateAdapter() {
        getParcelList(targetState)
        this.parcelAdapter.setParcels(parcelList)
        this.recyclerView.adapter!!.notifyDataSetChanged()
    }


    fun getPlannedParcelList(): ArrayList<ParcelEntity>? {
        return getParcelList(ParcelState.PLANNED)
    }


    /* Method that returns a list of dummy data */
    private fun getParcelList(state:Int): ArrayList<ParcelEntity>? {
        parcelList = ArrayList(ParcelRepository.shared.getByState(state))
        sortParcel()
        return parcelList
    }


    private fun sortParcel() {
        Collections.sort(parcelList) { x, y -> x.deliveryPosition - y.deliveryPosition }
    }
}
