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
import io.rover.rover.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.streams.share
import io.rover.rover.ui.ViewModelFactoryInterface
import kotlinx.android.parcel.Parcelize

class ExperienceViewModel(
    private val experienceId: String,
    private val networkService: NetworkServiceInterface,
    private val viewModelFactory: ViewModelFactoryInterface,
    private val icicle: Parcelable? = null
): ExperienceViewModelInterface {
    private var navigationViewModel: ExperienceNavigationViewModelInterface? = null

    // TODO: Perhaps separate fetch concern from toolbar hosting concern.

    override val state: Parcelable
        get() {
            // this is a slightly strange arrangement: since all state is really within a contained
            // view model (that only becomes available synchronously) we effectively assume we have
            // only our icicle state when that nested view model is not available.
            return if(navigationViewModel == null) {
                icicle ?: State(null)
            } else {
                State(
                    navigationViewModel?.state
                )
            }
        }

    private val actionSource = PublishSubject<Action>()

    private val actions = actionSource.share()

    private fun fetchExperience(): Publisher<NetworkResult<Experience>> =
        ({ callback: CallbackReceiver<NetworkResult<Experience>> -> networkService.fetchExperienceTask(ID(experienceId), callback) }).asPublisher()

    private val epic : Observable<ExperienceViewModelInterface.Event> =
        Observable.merge(
            actions.map { action ->
                when(action) {
                    Action.BackPressedBeforeExperienceReady -> {
                        // when view model isn't available (yet) but the user mashed the back button,
                        // just emit Exit immediately.
                        ExperienceViewModelInterface.Event.ExternalNavigation(ExperienceExternalNavigationEvent.Exit())
                    }
                }
            }.filterNulls(),
            fetchExperience()
                .flatMap { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Error-> Observable.just(
                            ExperienceViewModelInterface.Event.DisplayError(
                                networkResult.throwable.message ?: "Unknown"
                            )
                        )
                        is NetworkResult.Success -> {
                            val navigationViewModel = viewModelFactory.viewModelForExperienceNavigation(
                                networkResult.response, (state as State).navigationState
                            )
                            val experienceReadyEvent = ExperienceViewModelInterface.Event.ExperienceReady(navigationViewModel)

                            Observable.concat(
                                Observable.just(experienceReadyEvent),
                                navigationViewModel.events.map { navigationEvent ->
                                    when (navigationEvent) {
                                        // pass the ViewEvents further up to the surrounding activity.
                                        // Any external navigation events (exit, load web URI, change backlight)
                                        // from the navigation view model need to be passed up.
                                        // What will unsubscribe this when a new ExperienceNavigationViewModel
                                        // comes through?  For now not likely to happen because this view model is not re-bound.
                                        is ExperienceNavigationViewModelInterface.Event.ViewEvent -> ExperienceViewModelInterface.Event.ExternalNavigation(navigationEvent.event)
                                        is ExperienceNavigationViewModelInterface.Event.SetBacklightBoost -> ExperienceViewModelInterface.Event.SetBacklightBoost(navigationEvent.extraBright)
                                        else -> null
                                    }
                                }.filterNulls()
                            ).map {
                                this@ExperienceViewModel.log.v("Observed navigation View Event: $it")
                                it
                            }
                        }
                    }
                }.map { event ->
                    // side-effect!  Store newly navigationViewModel as state (TODO should be doOnNext)
                    if (event is ExperienceViewModelInterface.Event.ExperienceReady) {
                        log.v("Remembering experience view model.")
                        navigationViewModel = event.experienceNavigationViewModel
                    }

                    event
                }
            ).share()

    override val events: Observable<ExperienceViewModelInterface.Event> = epic.shareAndReplayTypesOnResubscribe(
        // ExperienceReady should be replayed to any new subscriber to make sure they are brought up to date.
        ExperienceViewModelInterface.Event.ExperienceReady::class.java
    )

    override fun pressBack() {
        if(navigationViewModel == null) {
            actionSource.onNext(Action.BackPressedBeforeExperienceReady)
        } else {
            navigationViewModel?.pressBack()
        }
    }

    enum class Action {
        /**
         * Back pressed before the experience navigation view model became available.  We can't
         * deliver the back press event to it as we would normally do; instead we'll handle this
         * case as an event ourselves and emit an Exit event for it instead.
         */
        BackPressedBeforeExperienceReady
    }

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class State(
        val navigationState: Parcelable? // TODO: see comment on ExperienceNavigationViewModelInterface.state
    ): Parcelable
}
