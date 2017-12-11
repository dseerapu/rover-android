package io.rover.rover.ui.views

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.platform.asAndroidUri
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

    private val viewCache: HashMap<ScreenViewModelInterface, ScreenView> = hashMapOf()

    fun getViewForScreenViewModel(screenViewModel: ScreenViewModelInterface): ScreenView {
        // TODO: manage the size of the cache.  we do not want too many active views (like, at most
        // two), since they are full Experience screens
        return viewCache[screenViewModel] ?: ScreenView(
            context
        ).apply {
            viewCache[screenViewModel] = this
            this.viewModel = screenViewModel
        }
    }

    override var viewModel: ExperienceViewModelInterface? = null
        set(experienceViewModel) {
            field = experienceViewModel

            field?.events?.subscribe( { event ->
                val wat = when(event) {
                    is ExperienceViewModelInterface.Event.WarpToScreen -> {
                        // find view for the given viewmodel
                        val newView = getViewForScreenViewModel(event.screenViewModel)

                        activeView.whenNotNull { removeView(it) }

                        addView(newView)
                        newView.visibility = View.VISIBLE
                    }
                    is ExperienceViewModelInterface.Event.GoForwardToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)

                        TransitionManager.beginDelayedTransition(this)
                        activeView.whenNotNull {
                            it.visibility = View.GONE
                            removeView(it)
                        }

                        addView(newView)
                        newView.visibility = View.VISIBLE
                    }
                    is ExperienceViewModelInterface.Event.GoBackwardToScreen -> {
                        val newView = getViewForScreenViewModel(event.screenViewModel)

                        TransitionManager.beginDelayedTransition(this)
                        activeView.whenNotNull {
                            it.visibility = View.GONE
                            removeView(it)
                        }

                        addView(newView)
                        newView.visibility = View.VISIBLE
                    }
                    is ExperienceViewModelInterface.Event.OpenExternalWebBrowser -> {
                        // TODO: this is possibly the wrong place to listen for this.
                        startActivity(
                            context,
                            Intent(
                                Intent.ACTION_VIEW,
                                event.uri.asAndroidUri()
                            ),
                            null
                        )
                    }
                    is ExperienceViewModelInterface.Event.Exit -> {
                        // TODO: this is definitely the wrong place to listen for this!
                        log.v("Gotta go back out, but can't lol")
                    }
                }
            }, { error -> throw error })
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // kick off a test transition
//        postDelayed({
//            TransitionManager.beginDelayedTransition(this)
//
//            viewOne.visibility = View.GONE
//            viewTwo.visibility = View.VISIBLE
//        }, 2000)
    }
}
