package com.deepmap.ebdemoapp.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class LevelSwitch(val levels: Int): RecyclerView.Adapter<LevelSwitchViewHolder>() {
    private val CLOSE_BUTTON = 1
    private val LEVEL_BUTTON = 2
    private val OPEN_SWITCH_BUTTON = 3

    var isExpanded = false
    var levelSwitchListener: LevelSwitchListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelSwitchViewHolder {
        return when(viewType) {
            LEVEL_BUTTON -> LevelSwitchLevelButton(parent, levels)
            CLOSE_BUTTON -> LevelSwitchOpenButton(parent)
            OPEN_SWITCH_BUTTON -> LevelSwitchOpenButton(parent)
            else -> LevelSwitchViewHolder(View(parent.context))
        }
    }

    override fun getItemCount(): Int = if (isExpanded) levels + 1 else 1

    override fun onBindViewHolder(holder: LevelSwitchViewHolder, position: Int) {
        if (isExpanded) {
            when (holder) {
                is LevelSwitchOpenButton -> {
                    holder.itemView.setOnClickListener {
                        expand(false)
                    }
                    holder.bind(false)
                }
                is LevelSwitchLevelButton -> {
                    holder.itemView.setOnClickListener {
                        levelSwitchListener?.onLevelClick(position - 1)
                    }
                    holder.bind(position - 1)
                }
            }
        } else {
            val openHolder = (holder as? LevelSwitchOpenButton) ?: return
            openHolder.bind(true)
            openHolder.itemView.setOnClickListener {
                expand(true)
            }
        }
    }

    private fun expand(isExpanded: Boolean) {
        notifyItemChanged(1)
        notifyItemRangeRemoved(1, itemCount)
        this.isExpanded = isExpanded
        notifyItemRangeInserted(1, itemCount)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isExpanded && position == 0 -> CLOSE_BUTTON
            !isExpanded -> OPEN_SWITCH_BUTTON
            isExpanded && position > 0 -> LEVEL_BUTTON
            else -> OPEN_SWITCH_BUTTON
        }
    }
}