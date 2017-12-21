package io.rover.rover.ui.views

import android.content.Context
import android.support.transition.Slide
import android.support.transition.TransitionManager
import android.support.transition.TransitionSet
import android.util.AttributeSet
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceNavigationViewModelInterface
import io.rover.rover.ui.viewmodels.ScreenViewModelInterface

/**
 * Navigation behaviour between screens of an Experience.
 */
class ExperienceNavigationView : FrameLayout, BindableView<ExperienceNavigationViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // TODO: implement an onCreate and if inEditMode() display a "Rover Experience" text view.

    private var activeView: ScreenView? = null

    /**
     * You must set [attachedWindow] on the Experience view before binding the view model.
     * This is needed for backlight control.
     */
    var attachedWindow: Window? = null

    private val viewCache: LruCache<ScreenViewModelInterface, ScreenView> = object : LruCache<ScreenViewModelInterface, ScreenView>(3) {
        override fun entryRemoved(evicted: Boolean, key: ScreenViewModelInterface?, oldValue: ScreenView?, newValue: ScreenView?) {
            removeView(oldValue)
        }
    }

    private fun getViewForScreenViewModel(screenViewModel: ScreenViewModelInterface): ScreenView {
        return viewCache[screenViewModel] ?: ScreenView(
            context
        ).apply {
            this@ExperienceNavigationView.addView(this)
            this.visibility = View.GONE
            viewCache.put(screenViewModel, this)
            this.viewModel = screenViewModel
        }
    }

    override var viewModel: ExperienceNavigationViewModelInterface? = null
        set(experienceViewModel) {
            if(attachedWindow == null) {
                throw RuntimeException("You must set the attached window on ExperienceNavigationView before binding it to a view model.")
            }
            field = experienceViewModel

            field?.events?.androidLifecycleDispose(this)?.subscribe( { event ->
                when(event) {

                    is ExperienceNavigationViewModelInterface.Event.WarpToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)
                        activeView?.visibility = View.GONE
                        newView.visibility = View.VISIBLE
                        activeView = newView
                    }
                    is ExperienceNavigationViewModelInterface.Event.GoForwardToScreen -> {
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
                    is ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen -> {
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
                    is ExperienceNavigationViewModelInterface.Event.SetBacklightBoost -> {
                        attachedWindow?.attributes = (attachedWindow?.attributes ?: WindowManager.LayoutParams()).apply {
                            screenBrightness = when(event.extraBright) {
                                true -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                                false -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                            }
                        }
                    }
                }
            }, { error -> throw error })
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedWindow = null
    }
}
