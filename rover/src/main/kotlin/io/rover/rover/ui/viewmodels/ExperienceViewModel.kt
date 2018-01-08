package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.ID
import io.rover.rover.core.logging.log
import io.rover.rover.services.network.NetworkResult
import io.rover.rover.services.network.NetworkServiceInterface
import io.rover.rover.streams.CallbackReceiver
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.Publisher
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.shareAndReplay
import io.rover.rover.streams.share
import io.rover.rover.ui.ViewModelFactoryInterface
import kotlinx.android.parcel.Parcelize

class ExperienceViewModel(
    private val experienceId: String,
    private val networkService: NetworkServiceInterface,
    private val viewModelFactory: ViewModelFactoryInterface,
    private val icicle: Parcelable? = null
): ExperienceViewModelInterface {
    private var experienceViewModel : ExperienceNavigationViewModelInterface? = null

    override val state: Parcelable
        get() {
            // this is a slightly strange arrangement: since all state is really within a contained
            // view model (that only becomes available synchronously) we effectively assume we have
            // only our icicle state when that nested view model is not available.
            return if(experienceViewModel == null) {
                icicle ?: State(null)
            } else {
                State(
                    experienceViewModel?.state
                )
            }
        }

    private val actions = PublishSubject<Action>()

    private fun fetchExperience(): Publisher<NetworkResult<Experience>> =
        ({ callback: CallbackReceiver<NetworkResult<Experience>> -> networkService.fetchExperienceTask(ID(experienceId), callback) }).asPublisher()

    private val fetchEpic : Observable<ExperienceViewModelInterface.Event> = fetchExperience()
        .map { networkResult ->
            when (networkResult) {
                is NetworkResult.Error-> ExperienceViewModelInterface.Event.DisplayError(networkResult.throwable.message ?: "Unknown")
                is NetworkResult.Success -> {
                    val viewModel = viewModelFactory.viewModelForExperienceNavigation(
                        networkResult.response, (state as State).navigationState
                    )

                    ExperienceViewModelInterface.Event.ExperienceReady(viewModel)
                }
            }
        }.map { event ->
            // side-effect!  Store newly experienceViewModel as state (TODO should be doOnNext)
            if(event is ExperienceViewModelInterface.Event.ExperienceReady) {
                log.v("Remembering experience view model.")
                experienceViewModel = event.experienceNavigationViewModel
            }
            event
        }.shareAndReplay(1)
        .flatMap { event ->
            if(event is ExperienceViewModelInterface.Event.ExperienceReady) {
                Observable.concat(
                    Observable.just(
                        ExperienceViewModelInterface.Event.ExperienceReady(
                            event.experienceNavigationViewModel
                        )
                    ),

                    // Any external navigation events (exit, load web URI, change backlight)
                    // from the navigation view model need to be passed up.
                    // What will unsubscribe this when a new ExperienceNavigationViewModel
                    // comes through?  For now not likely to happen because this view model is not re-bound.
                    event.experienceNavigationViewModel.events.map { navigationEvent ->
                        when(navigationEvent) {
                            is ExperienceNavigationViewModelInterface.Event.ViewEvent -> ExperienceViewModelInterface.Event.ViewEvent(navigationEvent.event)
                            else -> null
                        }
                    }.filterNulls()
                )
            } else {
                Observable.just(event)
            }
        }

    /**
     * The main behaviour
     */
    private val epic : Observable<ExperienceViewModelInterface.Event> = Observable.concat(
            fetchEpic,
                actions.map { action ->
                    when(action) {
                        Action.BackPressedWithoutViewModelAvailable -> {
                            // when view model isn't available (yet) but the mashed the back button,
                            // just emit Exit immediately.
                            ExperienceViewModelInterface.Event.ViewEvent(ExperienceViewEvent.Exit())
                        }
                    }
                }
        )

    override val events: Observable<ExperienceViewModelInterface.Event> = epic

    override fun pressBack() {
        if(experienceViewModel == null) {
            actions.onNext(Action.BackPressedWithoutViewModelAvailable)
        } else {
            experienceViewModel?.pressBack()
        }
    }

    enum class Action {
        /**
         * Back pressed before the navigation view model became available.  We can't deliver the
         * back press event to it as we would normally do; instead we'll handle this case as an
         * event ourselves and emit an Exit event for it instead.
         */
        BackPressedWithoutViewModelAvailable
    }

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class State(
        val navigationState: Parcelable? // TODO: see comment on ExperienceNavigationViewModelInterface.state
    ): Parcelable
}
