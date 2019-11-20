package de.dpd.vanassist.adapters

import de.dpd.vanassist.R
import de.dpd.vanassist.database.entity.ParcelEntity
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.util.parcel.ParcelState

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RadioButton
import de.dpd.vanassist.database.entity.CourierEntity
import de.dpd.vanassist.util.parcel.ParcelUtil
import de.dpd.vanassist.config.ParcelInformationAdapterConfig

import kotlinx.android.synthetic.main.parcel_information_card.view.*

class ParcelInformationAdapter(private val fragment: androidx.fragment.app.Fragment) : androidx.recyclerview.widget.RecyclerView.Adapter<ParcelInformationAdapter.ParcelInformationHolder>(){

    private lateinit var parcelList: ArrayList<ParcelEntity>
    private var expandedPosition = -1
    private lateinit var touchHelper : ItemTouchHelper
    private val api = VanAssistAPIController(fragment.activity as AppCompatActivity)
    private lateinit var courier: CourierEntity
    private var state = 0

    private lateinit var arrow : TextView

    fun setTouchHelper(touchHelper: ItemTouchHelper) {
        this.touchHelper = touchHelper
    }

    fun setParcels(parcelList: ArrayList<ParcelEntity>) {
        this.parcelList = parcelList
    }

    fun setState(state: Int) {
        this.state = state
    }

    fun setCourier(courier:CourierEntity) {
        this.courier = courier
    }

    fun getFragment(): androidx.fragment.app.Fragment {

        return this.fragment
    }

    /* Inflates the parcel view within the list
     * @params ViewGroup, viewType */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParcelInformationHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.parcel_information_card, parent, false)
        arrow = view.expand_icon

        return ParcelInformationHolder(view)
    }


    /* Gets the number of parcelList in the list */
    override fun getItemCount(): Int {
        return parcelList.size
    }


    /* Binds each parcel in the list to a view */
    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ParcelInformationHolder, position: Int) {

        val currentParcel = parcelList[position]

        holder.parcel = currentParcel
        holder.address.text = currentParcel.address
        holder.additionalClientAddressInfo.text = currentParcel.additionalAddressInformation
        holder.clientName.text = currentParcel.nameOfRecipient
        holder.additionalClientAddressInfo.text = currentParcel.additionalRecipientInformation
        holder.parcelSize.text = ParcelUtil.getParcelSize(currentParcel)

        /* create dialog for undo */
        holder.undo.setOnClickListener {
            val inflater = fragment.layoutInflater
            if (state == 1) {
                val dialogView = inflater.inflate(R.layout.undo_dialog_delivered, null)
                val builder1 = androidx.appcompat.app.AlertDialog.Builder(fragment.context!!)
                builder1.setView(dialogView)
                builder1.setTitle(fragment.getString(R.string.deliveries_dialog_title))
                builder1.setMessage(fragment.getString(R.string.deliveries_dialog_message))
                builder1.setCancelable(true)

                builder1.setPositiveButton(
                    "OK",
                    DialogInterface.OnClickListener { _, _ ->
                        // Accept configuration
                        val radioButtonOpen = dialogView.findViewById<RadioButton>(R.id.open_delivery_radio_button)!!
                        val radioButtonUnsuccess = dialogView.findViewById<RadioButton>(R.id.unsuccessful_delivery_radio_button)!!

                        if (radioButtonUnsuccess.isChecked) {
                            api.confirmParcelDeliveryFailure(currentParcel.id)
                            parcelList.removeAt(position)
                            notifyItemRemoved(position)
                            this.notifyDataSetChanged()
                        }
                        if (radioButtonOpen.isChecked) {
                            api.undoParcelDeliveryConfirmation(currentParcel)
                            parcelList.removeAt(position)
                            notifyItemRemoved(position)
                            this.notifyDataSetChanged()
                        }
                    })

                /* CourierEntity cancelled the dialog
                 * -> No action needed here */
                builder1.setNegativeButton(
                    fragment.getString(R.string.cancel),
                    DialogInterface.OnClickListener { _, _ ->
                    })


                builder1.create().show()
            } else if (state == ParcelState.DELIVERY_FAILURE) {
                val dialogView = inflater.inflate(R.layout.undo_dialog_undelivered, null)
                val builder1 = androidx.appcompat.app.AlertDialog.Builder(fragment.context!!)
                builder1.setView(dialogView)
                builder1.setTitle(fragment.getString(R.string.deliveries_dialog_title))
                builder1.setMessage(fragment.getString(R.string.deliveries_dialog_message))
                builder1.setCancelable(true)

                builder1.setPositiveButton(
                    fragment.getString(R.string.yes),
                    DialogInterface.OnClickListener { _, _ ->
                        /* Accept configuration */
                        val radioButtonOpen = dialogView.findViewById<RadioButton>(R.id.open_delivery_radio_button_2)!!
                        val radioButtonSuccess = dialogView.findViewById<RadioButton>(R.id.successful_delivery_radio_button)!!

                        if (radioButtonSuccess.isChecked) {
                            api.confirmParcelDeliverySuccess(currentParcel.id)
                            parcelList.removeAt(position)
                            notifyItemRemoved(position)
                            this.notifyDataSetChanged()

                        }
                        if (radioButtonOpen.isChecked) {
                            api.undoParcelDeliveryConfirmation(currentParcel)
                            parcelList.removeAt(position)
                            notifyItemRemoved(position)
                            this.notifyDataSetChanged()}
                    })

                /* CourierEntity cancelled the dialog
                 * -> No action needed here */
                builder1.setNegativeButton(
                    fragment.getString(R.string.cancel),
                    DialogInterface.OnClickListener { _, _ ->
                    })

                builder1.create().show()
            }
        }

        holder.lengthLabel.text = currentParcel.length.toString()
        holder.widthLabel.text = currentParcel.width.toString()
        holder.heightLabel.text = currentParcel.height.toString()
        holder.weightLabel.text = currentParcel.weight.toString() + ParcelInformationAdapterConfig.WEIGHT_ABBR

        /* set color based on mode */
        if (courier.darkMode) {
            holder.address.setTextColor(Color.WHITE)
            holder.clientName.setTextColor(Color.WHITE)
            holder.box.setImageResource(R.drawable.ic_dpd_box_while_slim)
        }
        else {
            holder.address.setTextColor(Color.BLACK)
            holder.clientName.setTextColor(Color.BLACK)
            holder.box.setColorFilter(Color.BLACK)
            holder.box.setImageResource(R.drawable.ic_dpd_box_black_slim)
        }

        val isExpanded = position == expandedPosition

        holder.detailsOnExpand.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.view.isActivated = isExpanded
        holder.view.setOnClickListener {
            if (isExpanded) {
                animateCollapse()
            } else {
                animateExpand()
            }
            expandedPosition = if (isExpanded) -1 else position

            TransitionManager.beginDelayedTransition(holder.detailsOnExpand, AutoTransition())
            notifyItemChanged(position)
        }

        val targetState = currentParcel.state
        if (targetState == ParcelState.PLANNED) {
            holder.view.reorder_parcel.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(holder)
                }
                false
            }
            holder.view.undo.isEnabled = false
            holder.view.undo.visibility = View.GONE
        } else {
            holder.view.reorder_parcel.isEnabled = false
            holder.view.reorder_parcel.visibility = View.GONE
        }
    }

    private fun animateExpand() {
        val rotate = RotateAnimation(ParcelInformationAdapterConfig.ANIM_DEGREE_360, ParcelInformationAdapterConfig.ANIM_DEGREE_180, RELATIVE_TO_SELF, ParcelInformationAdapterConfig.PIVOT_X_Y, RELATIVE_TO_SELF, ParcelInformationAdapterConfig.PIVOT_X_Y)
        rotate.duration = ParcelInformationAdapterConfig.ANIMATION_DURATION
        rotate.fillAfter = true
        arrow.animation = rotate
    }

    private fun animateCollapse() {
        val rotate = RotateAnimation(ParcelInformationAdapterConfig.ANIM_DEGREE_180, ParcelInformationAdapterConfig.ANIM_DEGREE_360, RELATIVE_TO_SELF, ParcelInformationAdapterConfig.PIVOT_X_Y, RELATIVE_TO_SELF, ParcelInformationAdapterConfig.PIVOT_X_Y)
        rotate.duration = ParcelInformationAdapterConfig.ANIMATION_DURATION
        rotate.fillAfter = true
        arrow.animation = rotate
    }

    fun onViewMoved(oldPosition: Int, newPosition: Int) {
        val targetParcel = parcelList[oldPosition]
        parcelList.removeAt(oldPosition)
        parcelList.add(newPosition, targetParcel)
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    fun onItemDismiss(position: Int){
        parcelList.removeAt(position)
        notifyItemRemoved(position)
    }


    class ParcelInformationHolder constructor(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        lateinit var parcel: ParcelEntity
        var address = view.findViewById<TextView>(R.id.address)!!
        var clientName = view.findViewById<TextView>(R.id.client_name)!!
        var additionalClientAddressInfo = view.findViewById<TextView>(R.id.additionalclientaddress_info)!!
        var detailsOnExpand = view.findViewById<RelativeLayout>(R.id.details_on_expand)!!
        var box = view.findViewById<ImageView>(R.id.imageView2)!!
        var lengthLabel = view.findViewById<TextView>(R.id.lenghtLabel)!!
        var widthLabel = view.findViewById<TextView>(R.id.widthLabel)!!
        var heightLabel = view.findViewById<TextView>(R.id.heightLabel)!!
        var weightLabel = view.findViewById<TextView>(R.id.weightLabel)!!
        var undo = view.findViewById<ImageView>(R.id.undo)!!
        var parcelSize = view.parcelsize
    }
}