package de.dpd.vanassist.adapters

import de.dpd.vanassist.R
import de.dpd.vanassist.database.entity.Parcel
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.util.parcel.ParcelStatus

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RadioButton
import de.dpd.vanassist.database.repository.CourierRepository

import kotlinx.android.synthetic.main.parcel_information_card.view.*

class ParcelInformationAdapter(private val fragment: Fragment) : RecyclerView.Adapter<ParcelInformationAdapter.ParcelInformationHolder>(){

    private lateinit var parcels: ArrayList<Parcel>
    private var expandedPosition = -1
    private lateinit var touchHelper : ItemTouchHelper
    private val api = VanAssistAPIController(fragment.activity as AppCompatActivity)
    private var state = 0

    private lateinit var arrow : TextView

    fun setTouchHelper(touchHelper: ItemTouchHelper) {
        this.touchHelper = touchHelper
    }

    fun setParcels(parcelList: ArrayList<Parcel>) {
        this.parcels = parcelList
    }

    fun setState(state: Int) {
        this.state = state
    }

    fun getFragment(): Fragment{

        return this.fragment
    }

    /**
     * Inflates the parcel view within the list
     * @params ViewGroup, viewType
     **/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParcelInformationHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.parcel_information_card, parent, false)
        arrow = view.expand_icon

        return ParcelInformationHolder(view)
    }


    /**
     * Gets the number of parcels in the list
     */
    override fun getItemCount(): Int {

        return parcels.size
    }


    /**
     * Binds each parcel in the list to a view
     */
    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ParcelInformationHolder, position: Int) {

        val currentParcel = parcels[position]

        holder.parcel = currentParcel
        holder.address.text = currentParcel.address
        holder.additionalClientAddressInfo.text = currentParcel.additionalAddressInformation
        holder.clientName.text = currentParcel.nameOfRecipient
        holder.additionalClientAddressInfo.text = currentParcel.additionalRecipientInformation
        holder.undo.setOnClickListener {
            //create dialog for undo
            val inflater = fragment.layoutInflater
            if (state == 1) {
                val dialogView = inflater.inflate(R.layout.undo_dialog_delivered, null)
                val builder1 = android.support.v7.app.AlertDialog.Builder(fragment.context!!)
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
                            parcels.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        if (radioButtonOpen.isChecked) {
                            api.undoParcelDeliveryConfirmation(currentParcel)
                            parcels.removeAt(position)
                            notifyItemRemoved(position)
                        }
                    })

                builder1.setNegativeButton(
                    fragment.getString(R.string.cancel),
                    DialogInterface.OnClickListener { _, _ ->
                        // User cancelled the dialog
                    })


                builder1.create().show()
            }
            else if (state == 2) {
                val dialogView = inflater.inflate(R.layout.undo_dialog_undelivered, null)
                val builder1 = android.support.v7.app.AlertDialog.Builder(fragment.context!!)
                builder1.setView(dialogView)
                builder1.setTitle(fragment.getString(R.string.deliveries_dialog_title))
                builder1.setMessage(fragment.getString(R.string.deliveries_dialog_message))
                builder1.setCancelable(true)

                builder1.setPositiveButton(
                    fragment.getString(R.string.yes),
                    DialogInterface.OnClickListener { _, _ ->
                        // Accept configuration
                        val radioButtonOpen = dialogView.findViewById<RadioButton>(R.id.open_delivery_radio_button_2)!!
                        val radioButtonSuccess = dialogView.findViewById<RadioButton>(R.id.successful_delivery_radio_button)!!

                        if (radioButtonSuccess.isChecked) {
                            api.confirmParcelDeliverySuccess(currentParcel.id)
                            parcels.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        if (radioButtonOpen.isChecked) {
                            api.undoParcelDeliveryConfirmation(currentParcel)
                            parcels.removeAt(position)
                            notifyItemRemoved(position)                        }
                    })

                builder1.setNegativeButton(
                    fragment.getString(R.string.cancel),
                    DialogInterface.OnClickListener { _, _ ->
                        // User cancelled the dialog
                    })

                builder1.create().show()
            }
        }

        val l = currentParcel.length.toString() //+ "cm"
        val w = currentParcel.width.toString() //+ "cm"
        val h = currentParcel.height.toString() //+ "cm"
        val wg = currentParcel.weight.toString() + "g"
        holder.lengthLabel.text = l
        holder.widthLabel.text = w
        holder.heightLabel.text = h
        holder.weightLabel.text = wg

        //set color based on mode
        val courierRepo = CourierRepository(this.fragment.context!!)
        val current = courierRepo.getCourier()

        if (current?.darkMode!!) {
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
        if (targetState == ParcelStatus.PLANNED) {
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
        val rotate = RotateAnimation(360f, 180f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f)
        rotate.duration = 300
        rotate.fillAfter = true
        arrow.animation = rotate
    }

    private fun animateCollapse() {
        val rotate = RotateAnimation(180f, 360f, RELATIVE_TO_SELF, 0.5f, RELATIVE_TO_SELF, 0.5f)
        rotate.duration = 300
        rotate.fillAfter = true
        arrow.animation = rotate
    }

    fun onViewMoved(oldPosition: Int, newPosition: Int) {
        val targetParcel = parcels[oldPosition]
        parcels.removeAt(oldPosition)
        parcels.add(newPosition, targetParcel)
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    fun onItemDismiss(position: Int){

        parcels.removeAt(position)
        notifyItemRemoved(position)
    }


    class ParcelInformationHolder constructor(val view: View) : RecyclerView.ViewHolder(view) {

        lateinit var parcel: Parcel
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
    }
}