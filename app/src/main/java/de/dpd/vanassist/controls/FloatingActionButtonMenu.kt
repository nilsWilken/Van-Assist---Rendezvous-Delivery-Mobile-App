package de.dpd.vanassist.controls

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FloatingActionButtonMenu : CoordinatorLayout {
    private val childFabs: MutableList<FloatingActionButton> = mutableListOf()
    private lateinit var mainFab: FloatingActionButton

    private lateinit var fabOpenAnim: Animation
    private lateinit var fabCloseAnim: Animation
    private lateinit var fadeInAnim: Animation
    private lateinit var fadeOutAnim: Animation

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init(attrs) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) { init(attrs) }

    private fun init(attributeSet: AttributeSet?) {
        val attrs = attributeSet ?: return

        mainFab = FloatingActionButton(context, attrs)
        childFabs.clear()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is FloatingActionButton)
                childFabs.add(child)
        }
    }
}