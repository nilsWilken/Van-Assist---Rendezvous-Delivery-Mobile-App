package com.deepmap.ebdemoapp.view

import android.view.LayoutInflater
import android.view.ViewGroup
import de.dpd.vanassist.R
import kotlinx.android.synthetic.main.level_button.view.*

class LevelSwitchLevelButton(parent: ViewGroup, private val levelCount: Int):
    LevelSwitchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.level_button, parent, false)) {
    fun bind(level: Int) {
        itemView.buttonText.text = (level + 1).toString()
        itemView.buttonBackground.setBackgroundResource(when (level) {
            0 -> {
                R.drawable.level_button_top
            }
            (levelCount - 1) -> {
                R.drawable.level_button_bottom
            }
            else -> {
                R.drawable.level_button_straight
            }
        })
    }
}