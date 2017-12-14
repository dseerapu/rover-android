package io.rover.rover.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface
import io.rover.rover.ui.viewmodels.StateOfButton

class ButtonBlockView : FrameLayout, LayoutableView<ButtonBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val disabledView: ButtonStateView = ButtonStateView(context)
    private val highlightedView: ButtonStateView = ButtonStateView(context)
    private val normalView: ButtonStateView = ButtonStateView(context)
    private val selectedView: ButtonStateView = ButtonStateView(context)

    private val textView: TextView = TextView(context)

    private val viewText: ViewText = ViewText(textView, AndroidRichTextToSpannedTransformer())

    private val viewBlock = ViewBlock(this, setOf())

    private val fauxRippleEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    init {
        addView(disabledView)
        addView(highlightedView)
        addView(normalView)
        addView(selectedView)
        addView(textView)

        setupRippleEffect()

        disabledView.visibility = View.INVISIBLE
        highlightedView.visibility = View.INVISIBLE
        normalView.visibility = View.INVISIBLE
        selectedView.visibility = View.INVISIBLE
    }

    @SuppressLint("NewApi")
    fun setupRippleEffect() {
        if(fauxRippleEffect) textView.elevation = 0f
    }

    override var viewModel: ButtonBlockViewModelInterface? = null
        @SuppressLint("NewApi")
        set(buttonBlockViewModel) {
            field = buttonBlockViewModel

            viewBlock.blockViewModel = buttonBlockViewModel

            disabledView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Disabled)
            normalView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Normal)
            highlightedView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Highlighted)
            selectedView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Selected)

            buttonBlockViewModel?.buttonEvents?.subscribe({ event ->
                when(event) {
                    is ButtonViewModelInterface.Event.DisplayState -> {
                        viewText.textViewModelInterface = event.viewModel

                        val viewStateBeingTransitionedTo = event.stateOfButton

                        log.v("Moving to state $viewStateBeingTransitionedTo")

                        val sourceView = currentlyActiveView()

                        val viewToTransitionTo = when(viewStateBeingTransitionedTo) {
                            StateOfButton.Disabled -> disabledView
                            StateOfButton.Normal -> normalView
                            StateOfButton.Highlighted -> highlightedView
                            StateOfButton.Selected -> selectedView
                        }

                        if(sourceView == null) {
                            // no prior state to transition from, no animation is appropriate.
                            viewToTransitionTo.visibility = View.VISIBLE
                        } else {
                            if(fauxRippleEffect) {
                                // ensure the one to transition to is on top because we will reveal it with a clip animation.

                                // Using negative elevations because that inhibits the shadows.
                                sourceView?.elevation = -2f
                                viewToTransitionTo.elevation = -1f

                                // make both visible
                                sourceView?.visibility = View.VISIBLE
                                viewToTransitionTo?.visibility = View.VISIBLE

                                // this isn't quite equivalent to a proper Material Design Ripple
                                // effect because there is no slight fade animation at the same time.
                                // I expect that two animators for the circle reveal and fade in
                                // would conflict.  A solution is surely possible but that would
                                // require more engineering investment.
                                val animation = ViewAnimationUtils.createCircularReveal(
                                    viewToTransitionTo,
                                    width / 2, height / 2,
                                    0f,
                                    Math.hypot(width / 2.0, height / 2.0).toFloat()
                                )
                                animation.addListener(object : AnimatorListenerAdapter() {
                                    // TODO: this must be unsubscribed when view is out-of-lifecycle and when we want to fire a new animation
                                    override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                                        sourceView?.visibility = View.INVISIBLE
                                    }
                                })
                                // ANDREW START HERE AND ADDRESS POTENTIAL LEAK ISSUES
                                animation.start()
                            } else {
                                // On older Android versions just snap.
                                sourceView?.visibility = View.INVISIBLE
                                viewToTransitionTo.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            },
                { error -> throw(RuntimeException("Button block view subscription to view model error", error)) }
            )
        }

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }

    private fun currentlyActiveView(): ButtonStateView? {
        val views = setOf(
            disabledView, normalView, highlightedView, selectedView
        )

        return views.firstOrNull() { it.visibility == View.VISIBLE }
    }
}
