package de.dpd.vanassist.controls

import androidx.recyclerview.widget.ItemTouchHelper
import de.dpd.vanassist.adapters.ParcelInformationAdapter
import de.dpd.vanassist.fragment.main.launchpad.ParcelListFragment
import de.dpd.vanassist.util.FragmentRepo


class DragCallback constructor(private val adapter: ParcelInformationAdapter) : ItemTouchHelper.Callback() {

    /**
     * method that sets the supported movements
     */


    var dragToPosition = -1
    var sourcePosition = -1

    override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, source: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
        if (source.itemViewType != target.itemViewType) {
            return false
        }
        dragToPosition = target.adapterPosition
        sourcePosition = source.adapterPosition

        // Notify the adapter of the move
        adapter.onViewMoved(source.adapterPosition, target.adapterPosition)
        return true
    }


    override fun onSelectedChanged(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?, actionState: Int) {
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

    override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, i: Int) {
        // Notify the adapter of the dismissal
        //adapter.onItemDismiss(viewHolder.adapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun interpolateOutOfBoundsScroll(recyclerView: androidx.recyclerview.widget.RecyclerView, viewSize: Int, viewSizeOutOfBounds: Int, totalSize: Int, msSinceStartScroll: Long): Int {
        return super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll)
    }
}