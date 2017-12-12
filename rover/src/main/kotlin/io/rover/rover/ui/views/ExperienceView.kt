package io.rover.rover.ui.views

import android.content.Context
import android.support.transition.Slide
import android.support.transition.TransitionManager
import android.support.transition.TransitionSet
import android.util.AttributeSet
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface
import io.rover.rover.ui.viewmodels.ScreenViewModelInterface

/**
 * Navigation behaviour between screens of an Experience.
 */
class ExperienceView: FrameLayout, BindableView<ExperienceViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private var activeView: ScreenView? = null

    private val viewCache: LruCache<ScreenViewModelInterface, ScreenView> = object : LruCache<ScreenViewModelInterface, ScreenView>(3) {
        override fun entryRemoved(evicted: Boolean, key: ScreenViewModelInterface?, oldValue: ScreenView?, newValue: ScreenView?) {
            removeView(oldValue)
        }
    }

    private fun getViewForScreenViewModel(screenViewModel: ScreenViewModelInterface): ScreenView {
        return viewCache[screenViewModel] ?: ScreenView(
            context
        ).apply {
            this@ExperienceView.addView(this)
            this.visibility = View.GONE
            viewCache.put(screenViewModel, this)
            this.viewModel = screenViewModel
        }
    }

    override var viewModel: ExperienceViewModelInterface? = null
        set(experienceViewModel) {
            field = experienceViewModel

            field?.events?.subscribe( { event ->
                when(event) {

                    is ExperienceViewModelInterface.Event.WarpToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)
                        activeView?.visibility = View.GONE
                        newView.visibility = View.VISIBLE
                        activeView = newView
                    }
                    is ExperienceViewModelInterface.Event.GoForwardToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)
                        newView.bringToFront()
                        newView.visibility = View.GONE

                        val set = TransitionSet().apply {
                            activeView.whenNotNull { activeView ->
                                addTransition(
                                    Slide(
                                        Gravity.START
                                    ).addTarget(activeView)
                                )
                            }
                            addTransition(
                                Slide(
                                    Gravity.END
                                ).addTarget(newView)
                            )
                        }

                        TransitionManager.beginDelayedTransition(this, set)
                        newView.visibility = View.VISIBLE
                        activeView.whenNotNull { it.visibility = View.GONE }

                        activeView = newView
                    }
                    is ExperienceViewModelInterface.Event.GoBackwardToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)
                        newView.bringToFront()
                        newView.visibility = View.GONE

                        val set = TransitionSet().apply {
                            addTransition(
                                Slide(
                                    Gravity.START
                                ).addTarget(newView)
                            )
                            activeView.whenNotNull { activeView ->
                                addTransition(
                                    Slide(
                                        Gravity.END
                                    ).addTarget(activeView)
                                )
                            }
                        }

                        TransitionManager.beginDelayedTransition(this, set)
                        activeView.whenNotNull {
                            it.visibility = View.GONE
                        }
                        newView.visibility = View.VISIBLE

                        activeView = newView
                    }
                }
            }, { error -> throw error })
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }
}
