package io.rover.rover.ui.views

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.util.AttributeSet
import io.rover.rover.core.logging.log
import io.rover.rover.streams.androidLifecycleDispose
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.viewmodels.ExperienceViewModelInterface

class ExperienceView: CoordinatorLayout, BindableView<ExperienceViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val experienceNavigationView: ExperienceNavigationView = ExperienceNavigationView(context)

    init {
        addView(
            experienceNavigationView
        )
    }

    override var viewModel: ExperienceViewModelInterface? = null
        set(experienceViewModel) {
            field = experienceViewModel

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
                }
            }, { error ->
                throw error
            })
        }
}
