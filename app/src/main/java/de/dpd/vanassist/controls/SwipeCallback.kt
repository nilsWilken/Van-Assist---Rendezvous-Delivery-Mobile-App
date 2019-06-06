package de.dpd.vanassist.controls

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import de.dpd.vanassist.R
import de.dpd.vanassist.adapters.ParcelInformationAdapter

class SwipeCallback constructor(private val pAdapter: ParcelInformationAdapter) : ItemTouchHelper.Callback() {

    private lateinit var background: ColorDrawable
    private lateinit var icon: Drawable

    /**
     * method that sets the available movements
     */

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {

        return makeMovementFlags(0, ItemTouchHelper.LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    /**
     * notifies the adapter that a remove has been made
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        pAdapter.onItemDismiss(viewHolder.adapterPosition)
        //pAdapter.apiCtrl.undoParcelDeliveryConfirmation()
    }

    override fun isItemViewSwipeEnabled(): Boolean {

        return true
    }

    override fun isLongPressDragEnabled(): Boolean {

        return false
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        background = ColorDrawable(Color.rgb(236, 236, 236))
        icon = ContextCompat.getDrawable(pAdapter.getFragment().context!!, R.drawable.ic_undo_red_50dp)!!

        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val inHeight = icon.intrinsicHeight
        val inWidth = icon.intrinsicWidth

        // Draw the red delete background

        //background = ColorDrawable(Color.rgb(135, 206, 250))

        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(canvas)

        // Calculate position of delete icon

        val iconTop = itemView.top + (itemHeight - inHeight) / 2
        val iconMargin = (itemHeight - inHeight) / 2
        val iconLeft = itemView.right - iconMargin - inWidth
        val iconRight = itemView.right - iconMargin
        val iconBottom = iconTop + inHeight

        // Draw the delete icon
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        icon.draw(canvas)

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}