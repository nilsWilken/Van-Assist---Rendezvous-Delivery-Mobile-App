package com.deepmap.ebdemoapp.view

import android.view.LayoutInflater
import android.view.ViewGroup
import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.level_switch_button.view.*

class LevelSwitchOpenButton(parent: ViewGroup): LevelSwitchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.level_switch_button, parent, false)) {
    fun bind(isExpanded: Boolean) {
        if (!isExpanded) {
            itemView.buttonBackground.setBackgroundResource(R.drawable.close_button_background)
            itemView.buttonIcon.setImageResource(R.drawable.ic_action_name)
        } else {
            itemView.buttonBackground.setBackgroundResource(R.drawable.level_button_rounded)
            itemView.buttonIcon.setImageResource(R.drawable.layer_button)
        }
    }
}