package io.rover.rover.ui.experience

import android.content.Context
import android.os.Build
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.Menu
import android.view.Window
import android.view.WindowManager
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.Observable
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.experience.navigation.ExperienceNavigationView
import io.rover.rover.ui.experience.toolbar.ViewExperienceToolbar
import io.rover.rover.ui.experience.concerns.BindableView

class ExperienceView : CoordinatorLayout, BindableView<ExperienceViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var toolbar: Toolbar? = null

    private val experienceNavigationView: ExperienceNavigationView = ExperienceNavigationView(context)

    private val appBarLayout = AppBarLayout(context)

    init {
        addView(
            experienceNavigationView
        )

        (experienceNavigationView.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
        }

        val appBarLayout = appBarLayout
        addView(appBarLayout)
        (appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.WRAP_CONTENT
        }
    }

    interface ToolbarHost {
        /**
         * The ExperiencesView will generate the toolbar and lay it out within it's own view.
         *
         * However, for it to work completely, it needs to be set as the Activity's (Fragment?)
         * toolbar.
         *
         * In response to this, the Activity will provide an ActionBar (and then, after a small
         * delay, a Menu).
         */
        fun setToolbarAsActionBar(toolbar: Toolbar): Observable<Pair<ActionBar, Menu>>

        fun provideWindow(): Window
    }

    var toolbarHost: ToolbarHost? = null
        set(host) {
            field = host
            // TODO: I need an event for when menu arrives, not just pull it.

            originalStatusBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                host?.provideWindow()?.statusBarColor ?: 0
            } else 0
        }

    var originalStatusBarColor: Int = 0 // this is set as a side-effect of the attached window

    private fun connectToolbar(newToolbar: Toolbar) {
        toolbar.whenNotNull { appBarLayout.removeView(it) }

        appBarLayout.addView(newToolbar)
        (newToolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        }

        toolbar = newToolbar
    }

    private val viewExperienceToolbar by lazy {
        val toolbarHost = toolbarHost ?: throw RuntimeException("You must set the ToolbarHost up on ExperienceView before binding the view to a view model.")
        ViewExperienceToolbar(
            this,
            toolbarHost.provideWindow(),
            this.context,
            toolbarHost
        )
    }

    override var viewModel: ExperienceViewModelInterface? = null
        set(experienceViewModel) {
            if (viewModel != null) {
                // sadly have to add this invariant because of complexity dealing with the toolbar.
                // May fix it later as required.
                throw RuntimeException("ExperienceView does not support being re-bound to a new ExperienceViewModel.")
            }
            field = experienceViewModel
            val toolbarHost = toolbarHost
                ?: throw RuntimeException("You must set the ToolbarHost up on ExperienceView before binding the view to a view model.")

            experienceNavigationView.viewModel = null

            if (experienceViewModel != null) {
                experienceViewModel.events.androidLifecycleDispose(this).subscribe({ event ->
                    when (event) {
                        is ExperienceViewModelInterface.Event.ExperienceReady -> {
                            experienceNavigationView.viewModel = event.experienceNavigationViewModel
                        }
                        is ExperienceViewModelInterface.Event.SetActionBar -> {
                            // regenerate and replace the toolbar

                            val newToolbar = viewExperienceToolbar.setViewModelAndReturnToolbar(
                                event.toolbarViewModel
                            )
                            connectToolbar(newToolbar)
                        }
                        is ExperienceViewModelInterface.Event.DisplayError -> {
                            Snackbar.make(this, "Problem: ${event.message}", Snackbar.LENGTH_LONG).show()
                            log.w("Unable to retrieve experience: ${event.message}")
                        }
                        is ExperienceViewModelInterface.Event.SetBacklightBoost -> {
                            toolbarHost.provideWindow().attributes = (toolbarHost.provideWindow().attributes ?: WindowManager.LayoutParams()).apply {
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
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        toolbarHost = null
    }
}
