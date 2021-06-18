package de.dpd.vanassist.adapters

import android.os.Parcel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginTop
import de.dpd.vanassist.R
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.controls.SwipeButton
import de.dpd.vanassist.database.entity.CourierEntity
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.database.repository.ParcelRepository
import de.dpd.vanassist.intelligence.dynamicContent.DynamicContent
import de.dpd.vanassist.intelligence.gamification.GamificationMode
import de.dpd.vanassist.intelligence.sizeDependentWaiting.SizeDependentWaiting
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.toast.Toast
import kotlinx.android.synthetic.main.parcel_information_card_bottom_sheet.view.*
import androidx.fragment.app.Fragment.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.dpd.vanassist.fragment.main.map.MapFragmentOld

class ParcelBottomSheetAdapter(private val fragment: androidx.fragment.app.Fragment, private var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>) : androidx.recyclerview.widget.RecyclerView.Adapter<ParcelBottomSheetAdapter.ParcelBottomSheetInformationHolder>() {

    private lateinit var parcelList: ArrayList<ParcelEntity>
    private val api = VanAssistAPIController(fragment.activity as AppCompatActivity, fragment.requireContext())
    private lateinit var courier: CourierEntity
    private var state = 0
    //private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    fun setParcels(parcelList: ArrayList<ParcelEntity>) {
        this.parcelList = parcelList
    }

    fun setState(state: Int) {
        this.state = state
    }

    fun setCourier(courier: CourierEntity) {
        this.courier = courier
    }

    fun setBottomSheetBehavior(bottomSheetBehavior: BottomSheetBehavior<LinearLayout>) {
        this.bottomSheetBehavior = bottomSheetBehavior
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParcelBottomSheetAdapter.ParcelBottomSheetInformationHolder {
        var view: View? = null
        if(this.state == 0) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.parcel_overview_card_bottom_sheet, parent, false)
        }else {
            view = LayoutInflater.from(parent.context).inflate(R.layout.parcel_information_card_bottom_sheet, parent, false)
        }

        val holder = ParcelBottomSheetInformationHolder(view)

        if(state == 0) {
            holder.address = view.findViewById<TextView>(R.id.overview_bottom_sheet_street_text_view)
            holder.clientName = view.findViewById<TextView>(R.id.overview_bottom_sheet_recipient_name_text_view)
            holder.clientNameAdditionalInformation = view.findViewById<TextView>(R.id.overview_bottom_sheet_recipient_name__additional_information_text_view)
            holder.additionalClientAdressInfo = view.findViewById<TextView>(R.id.overview_bottom_sheet_street_additional_information_text_view)
        } else {
            holder.address = view.findViewById<TextView>(R.id.bottom_sheet_street_text_view_v2)
            holder.clientName = view.findViewById<TextView>(R.id.bottom_sheet_recipient_name_text_view_v2)
            holder.additionalClientAdressInfo = view.findViewById<TextView>(R.id.bottom_sheet_street_additional_information_text_view_v2)
            holder.clientNameAdditionalInformation = view.findViewById<TextView>(R.id.bottom_sheet_recipient_name__additional_information_text_view_v2)


        }

        return holder
    }

    override fun onBindViewHolder(holder: ParcelBottomSheetInformationHolder, position: Int) {
        val currentParcel = parcelList[position]

        holder.parcel = currentParcel

        holder.address.text = currentParcel.address
        holder.clientName.text = currentParcel.nameOfRecipient

        if(currentParcel.additionalRecipientInformation != null) {
            holder.clientNameAdditionalInformation.text = currentParcel.additionalRecipientInformation
        } else {
            holder.clientNameAdditionalInformation.visibility = View.GONE
            holder.clientNameAdditionalInformation.text = "TEST"
        }

        if(currentParcel.city != null) {
            holder.additionalClientAdressInfo.text = currentParcel.city
        } else {
            holder.additionalClientAdressInfo.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int {
        return parcelList.size
    }


    class ParcelBottomSheetInformationHolder constructor(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        lateinit var parcel: ParcelEntity
        lateinit var address: TextView
        lateinit var clientName: TextView
        lateinit var additionalClientAdressInfo: TextView
        lateinit var clientNameAdditionalInformation: TextView

    }

}