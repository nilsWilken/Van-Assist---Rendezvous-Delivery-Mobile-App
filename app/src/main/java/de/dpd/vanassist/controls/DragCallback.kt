package de.dpd.vanassist.controls

import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.RecyclerView
import de.dpd.vanassist.adapters.ParcelInformationAdapter
import de.dpd.vanassist.fragment.main.ParcelListFragment
import de.dpd.vanassist.util.FragmentRepo


class DragCallback constructor(private val adapter: ParcelInformationAdapter) : ItemTouchHelper.Callback() {

    /**
     * method that sets the supported movements
     */


    var dragToPosition = -1
    var sourcePosition = -1

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (source.itemViewType != target.itemViewType) {
            return false
        }
        dragToPosition = target.adapterPosition
        sourcePosition = source.adapterPosition

        // Notify the adapter of the move
        adapter.onViewMoved(source.adapterPosition, target.adapterPosition)
        return true
    }


    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {

            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                if(dragToPosition != -1 && sourcePosition != -1) {
                    val parcel = ParcelListFragment.parcelList[dragToPosition]
                    val parcelId = parcel.id
                    val newPos = dragToPosition
                    if(FragmentRepo.parcelListFragment != null) {
                        FragmentRepo.parcelListFragment!!.changeParcelOrder(parcelId, (newPos+1))
                    }
                    sourcePosition = -1
                    dragToPosition = -1
                }
            }
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        // Notify the adapter of the dismissal
        //adapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun interpolateOutOfBoundsScroll(recyclerView: RecyclerView, viewSize: Int, viewSizeOutOfBounds: Int, totalSize: Int, msSinceStartScroll: Long): Int {
        return super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll)
    }
}