package io.rover.rover.ui.views

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import io.rover.rover.core.logging.log
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceNavigationViewModelInterface
import io.rover.rover.ui.viewmodels.ExperienceViewEvent
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface

class ExperienceView: FrameLayout, BindableView<ExperienceViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val experienceNavigationView: ExperienceNavigationView = ExperienceNavigationView(context)

    init {

//        addView(
//           toolbar
//        )
//        toolbar.title = "WAT"
//        (toolbar.layoutParams as CoordinatorLayout.LayoutParams).apply {
//            width = LayoutParams.MATCH_PARENT
//            height = LayoutParams.WRAP_CONTENT
//        }


        addView(
            experienceNavigationView
        )

//        (experienceNavigationView.layoutParams as CoordinatorLayout.LayoutParams).apply {
//            behavior = AppBarLayout.ScrollingViewBehavior()
//            width = LayoutParams.MATCH_PARENT
//            height = LayoutParams.MATCH_PARENT
//        }
//
//        val appBarLayout = AppBarLayout(context)
//        addView(appBarLayout)
//        (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
//            width = LayoutParams.MATCH_PARENT
//            height = LayoutParams.WRAP_CONTENT
//        }
//        appBarLayout.addView(toolbar)
//        (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
//            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
//        }
    }

    /**
     * You must set [attachedWindow] on the Experience view before binding the view model.
     * This is needed for backlight control.
     */
    var attachedWindow: Window? = null

    override var viewModel: ExperienceViewModelInterface? = null
        set(experienceViewModel) {
            field = experienceViewModel
            if(attachedWindow == null) {
                throw RuntimeException("You must set the attached window on ExperienceView before binding it to a view model.")
            }

            experienceNavigationView.viewModel = null

            experienceViewModel?.events?.androidLifecycleDispose(this)?.subscribe({ event ->
                when(event) {
                    is ExperienceViewModelInterface.Event.ExperienceReady -> {
                        experienceNavigationView.viewModel = event.experienceNavigationViewModel
                    }
                    is ExperienceViewModelInterface.Event.DisplayError -> {
                        Snackbar.make(this, "Problem: ${event.message}", Snackbar.LENGTH_LONG).show()
                        log.w("Unable to retrieve experience: ${event.message}")
                    }
                    is ExperienceViewModelInterface.Event.ViewEvent -> {
                        when(event.event) {
                            is ExperienceViewEvent.SetBacklightBoost -> {
                                attachedWindow?.attributes = (attachedWindow?.attributes ?: WindowManager.LayoutParams()).apply {
                                    screenBrightness = when (event.event.extraBright) {
                                        true -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                                        false -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                                    }
                                }
                            }
                        }
                    }

                }
            }, { error ->
                throw error
            })
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedWindow = null
    }
}
