package io.rover.rover.ui.views

import android.content.Context
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

/**
 *
 *
 * This will be the view that actually features some sort of navigation between screens.
 *
 * Question:
 * a) embed the flow in the view (view model) <-- CHOOSING THIS ONE
 * b) emit navigation messages which should ultimately be bound to some sort of navigation flow
 *    provider (maybe a fragment manager?)
 */
class ExperienceView: FrameLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // just testing a transition between two views for now:
    
    private val viewOne = TextView(context).apply { text = "ONE" }
    private val viewTwo = TextView(context).apply { text = "TWO" }

    init {
        viewOne.visibility = View.VISIBLE
        viewTwo.visibility = View.GONE

        addView(viewOne)
        addView(viewTwo)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // kick off a test transition
        postDelayed({
            TransitionManager.beginDelayedTransition(this)

            viewOne.visibility = View.GONE
            viewTwo.visibility = View.VISIBLE
        }, 2000)
    }
}
