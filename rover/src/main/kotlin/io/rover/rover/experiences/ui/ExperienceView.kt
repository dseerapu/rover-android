package io.rover.rover.experiences.ui

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
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.experiences.ui.navigation.ExperienceNavigationView
import io.rover.rover.experiences.ui.toolbar.ViewExperienceToolbar
import io.rover.rover.experiences.ui.concerns.BindableView
import io.rover.rover.experiences.ui.containers.StandaloneExperienceHostActivity

/**
 * Embed this view to include a Rover Experience in a layout.
 *
 * Most applications will likely want to use [StandaloneExperienceHostActivity] and
 * [ExperienceFragment] to display an Experience, but for more custom setups (say, tablet-enabled
 * single-activity apps that avoid fragments), you can embed [ExperienceView] directly.
 *
 * In order to display an Experience, use the implementation of
 * [ViewModelFactoryInterface.viewModelForExperience] to create an instance of the needed Experience
 * view model, and then bind it to the view model with setViewModel.
 *
 * Note about Android state restoration: Rover SDK views handle state saving & restoration through
 * their view models, so you will need store a Parcelable on behalf of ExperienceView and
 * [ExperienceViewModel] (grabbing the state Parcelable from the view model at save time and
 * restoring it by passing it to the view model factory at restart time).
 *
 * See [StandaloneExperienceHostActivity] for an example of how to integrate.
 */
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

    private var originalStatusBarColor: Int = 0 // this is set as a side-effect of the attached window

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

    override var viewModel: ExperienceViewModelInterface? by ViewModelBinding(false) { viewModel, subscriptionCallback ->
        // sadly have to set rebindingAllowed to be false because of complexity dealing with the
        // toolbar. May fix it later as required. TODO: put a note here about note why this is?

        val toolbarHost = toolbarHost
            ?: throw RuntimeException("You must set the ToolbarHost up on ExperienceView before binding the view to a view model.")

        experienceNavigationView.viewModel = null

        if (viewModel != null) {
            viewModel.events.androidLifecycleDispose(this).subscribe({ event ->
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
                        toolbarHost.provideWindow().attributes = (toolbarHost.provideWindow().attributes
                            ?: WindowManager.LayoutParams()).apply {
                            screenBrightness = when (event.extraBright) {
                                true -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                                false -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                            }
                        }
                    }
                }
            }, { error ->
                throw error
            }, { subscription -> subscriptionCallback(subscription) })
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        toolbarHost = null
    }
}
