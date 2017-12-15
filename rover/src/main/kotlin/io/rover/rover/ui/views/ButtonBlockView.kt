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
import io.rover.rover.streams.Observable
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface
import io.rover.rover.ui.viewmodels.StateOfButton
import java.util.*

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

        // start all the layer views at a low elevation
        disabledView.z = -2f
        highlightedView.z = -2f
        normalView.z = -2f
        selectedView.z = -2f
    }

    private var activeAnimator: Animator? = null
    private var queuedAnimationTasks: Queue<ButtonTransitionAnimation> = LinkedList()

    private fun executeAnimationOperation(operation: ButtonTransitionAnimation) {
        operation().apply {
            activeAnimator = this
            log.v("Adding listener")
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    log.v("2 at least started")
                }

                override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                    log.v("2")

//                    // remove myself:
//                    if(queuedAnimationTasks.poll() != operation) {
//                        throw RuntimeException("Animation operation list got out of alignment with itself. Race?")
//                    }

                    activeAnimator = null

                    // chain to next animation task if there is one.
                    val op = queuedAnimationTasks.poll()
                    if (op != null) {
                        executeAnimationOperation(operation)
                    }
                }
            })
            start()
        }
    }

    private fun queueAnimatorOperation(operation: ButtonTransitionAnimation) {
        log.v("Current queue depth is ${queuedAnimationTasks.size}")

        // TODO: because the Animator uses a background thread, could this be an issue? I don't
        // think so, because the animation end callback should arrive on the main thread.  although
        // maybe there's still a race because perhaps the poll() might happen before the insertion,
        // meaning that the animation will just sit in the queue.

        if(activeAnimator != null && activeAnimator?.isRunning == true) {
            queuedAnimationTasks.add(operation)
        } else {
            executeAnimationOperation(operation)
        }
    }

    override fun onAnimationEnd() {
        super.onAnimationEnd()
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

                                queueAnimatorOperation {

                                    // ensure the one to transition to is on top because we will reveal it with a clip animation.

                                    // Using negative elevations because that inhibits the shadows.
                                    sourceView.elevation = -2f
                                    viewToTransitionTo.elevation = -1f

                                    // make both visible
                                    sourceView.visibility = View.VISIBLE
                                    viewToTransitionTo.visibility = View.VISIBLE

                                    // So, this is very racy.  Example: if the Normal event arrives
                                    // while the Highlight animation is still running, it means that the
                                    // Normal button is actually still visible because it hasn't been
                                    // called on Animation end.  Probably best to cancel the animation,
                                    // ensure that we're at end-stage, and then continue.  We don't need
                                    // multiple propagating ripples at once.

                                    // Only one problem with that: we're snapping to the end state,
                                    // which causes a nasty flicker.  maybe we should queue the
                                    // animations? yeah, let's queue.

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

                                        override fun onAnimationStart(animation: Animator?) {
                                            log.v("1 at least started")
                                        }

                                        override fun onAnimationCancel(animation: Animator?) {
                                            log.v("1 cancelled?")
                                        }



                                        override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                                            log.v("1")
                                            sourceView.visibility = View.INVISIBLE
                                        }
                                    })
                                    // TODO: ADDRESS POTENTIAL LEAK ISSUES
                                    // TODO: the Selected event isn't coming through
                                    // TODO:
                                    animation
                                }

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
        log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }

    private fun currentlyActiveView(): ButtonStateView? {
        val views = listOf(
            disabledView, normalView, highlightedView, selectedView
        )

        return views.firstOrNull() { it.visibility == View.VISIBLE }
    }
}

typealias ButtonTransitionAnimation = () -> Animator