package io.rover.rover.plugins.userexperience.experience.blocks.button

import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.subscribe
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableView
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.ViewBlock
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.ViewText

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
        if (fauxRippleEffect) textView.elevation = 0f

        // start all the layer views at a low elevation
        disabledView.z = -2f
        highlightedView.z = -2f
        normalView.z = -2f
        selectedView.z = -2f
    }

    private var activeAnimator: Animator? = null

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
                when (event) {
                    is ButtonViewModelInterface.Event.DisplayState -> {
                        viewText.textViewModel = event.viewModel

                        val viewStateBeingTransitionedTo = event.stateOfButton

                        log.v("Moving to state $viewStateBeingTransitionedTo")

                        val sourceView = currentlyActiveView

                        val viewToTransitionTo = when (viewStateBeingTransitionedTo) {
                            StateOfButton.Disabled -> disabledView
                            StateOfButton.Normal -> normalView
                            StateOfButton.Highlighted -> highlightedView
                            StateOfButton.Selected -> selectedView
                        }

                        if (sourceView == null || !viewToTransitionTo.isAttachedToWindow) {
                            // no prior state to transition from, or the views are not currently
                            // attached, then no animation is appropriate.
                            viewToTransitionTo.visibility = View.VISIBLE
                            currentlyActiveView = viewToTransitionTo
                        } else {
                            if (fauxRippleEffect && event.animate) {
                                activeAnimator?.end()

                                // ensure the one to transition to is on top because we will reveal
                                // it with a clip animation.

                                // Using negative elevations because that inhibits the shadows.
                                sourceView.elevation = -2f
                                viewToTransitionTo.elevation = -1f

                                // all the other views that are not either of the current views in
                                // operation should be invisible.
                                allStateLayerViews.filter { it != viewToTransitionTo && it != sourceView }.forEach { it.visibility = View.INVISIBLE }

                                // make both visible
                                sourceView.visibility = View.VISIBLE
                                viewToTransitionTo.visibility = View.VISIBLE

                                // this isn't quite equivalent to a proper Material Design Ripple
                                // effect because there is no slight fade animation at the same time.
                                // I expect that two animators for the circle reveal and fade in
                                // would conflict.  A solution is surely possible but that would
                                // require more engineering investment.
                                val revealOutwards = ViewAnimationUtils.createCircularReveal(
                                    viewToTransitionTo,
                                    width / 2, height / 2,
                                    0f,
                                    Math.hypot(width / 2.0, height / 2.0).toFloat()
                                )

                                val animator = if (event.selfRevert) {
                                    AnimatorSet().apply {
                                        play(revealOutwards).before(
                                            ViewAnimationUtils.createCircularReveal(
                                                viewToTransitionTo,
                                                width / 2, height / 2,
                                                Math.hypot(width / 2.0, height / 2.0).toFloat(),
                                                0f
                                            )
                                        )
                                    }
                                } else revealOutwards

                                // workaround for Animator listeners not working reliably. For the
                                // non reversion case it's just a performance enhancement to ovoid
                                // overdraw by setting the view invisible, and for the reversion
                                // case it's important to hide the target view once the animation
                                // completes or it will simply pop back into view.  Works by
                                // dead-reckoning the animation time. :(
                                postDelayed({
                                    if (activeAnimator == animator) {
                                        if (event.selfRevert) {
                                            viewToTransitionTo.visibility = View.INVISIBLE
                                        } else {
                                            sourceView.visibility = View.INVISIBLE
                                        }
                                    }
                                }, animator.totalDuration)

                                // TODO: ADDRESS POTENTIAL LEAK ISSUES
                                animator.start()
                                if (!event.selfRevert) currentlyActiveView = viewToTransitionTo
                                activeAnimator = animator
                            } else {
                                // On older Android versions just snap.
                                sourceView.visibility = View.INVISIBLE
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
        // log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }

    private var currentlyActiveView: ButtonStateView? = null

    private val allStateLayerViews = setOf(disabledView, normalView, highlightedView, selectedView)
}
