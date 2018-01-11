package io.rover.rover.ui.views

import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.Window
import android.view.WindowManager
import io.rover.rover.core.logging.log
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface

class ExperienceView: CoordinatorLayout, BindableView<ExperienceViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    val toolbar: Toolbar = Toolbar(context)

    private val experienceNavigationView: ExperienceNavigationView = ExperienceNavigationView(context)

    private val viewToolbar by lazy {
        ViewExperienceToolbar(toolbar, attachedWindow!!)
    }

    init {
        addView(
            experienceNavigationView
        )

        (experienceNavigationView.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
        }

        val appBarLayout = AppBarLayout(context)
        addView(appBarLayout)
        (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.WRAP_CONTENT
        }
        appBarLayout.addView(toolbar)
        (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        }
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
            viewToolbar.experienceToolbarViewModel = null

            experienceViewModel?.events?.androidLifecycleDispose(this)?.subscribe({ event ->
                when(event) {
                    is ExperienceViewModelInterface.Event.ExperienceReady -> {
                        experienceNavigationView.viewModel = event.experienceNavigationViewModel
                        viewToolbar.experienceToolbarViewModel = event.experienceNavigationViewModel
                    }
                    is ExperienceViewModelInterface.Event.DisplayError -> {
                        Snackbar.make(this, "Problem: ${event.message}", Snackbar.LENGTH_LONG).show()
                        log.w("Unable to retrieve experience: ${event.message}")
                    }
                    is ExperienceViewModelInterface.Event.SetBacklightBoost -> {
                        attachedWindow?.attributes = (attachedWindow?.attributes ?: WindowManager.LayoutParams()).apply {
                            screenBrightness = when (event.extraBright) {
                                true -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                                false -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
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
