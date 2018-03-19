package io.rover.rover.experiences.ui

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.domain.ID
import io.rover.rover.core.logging.log
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.graphql.GraphQlApiServiceInterface
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.core.streams.share
import io.rover.rover.experiences.ui.navigation.ExperienceExternalNavigationEvent
import io.rover.rover.experiences.ui.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.platform.whenNotNull
import kotlinx.android.parcel.Parcelize

class ExperienceViewModel(
    private val experienceId: String,
    private val campaignId: String?,
    private val graphQlApiService: GraphQlApiServiceInterface,
    private val resolveNavigationViewModel: (experience: Experience, icicle: Parcelable?) -> ExperienceNavigationViewModelInterface,
    private val icicle: Parcelable? = null
) : ExperienceViewModelInterface {
    private var navigationViewModel: ExperienceNavigationViewModelInterface? = null

    // TODO: Perhaps separate fetch concern from toolbar hosting concern.

    override val state: Parcelable
        get() {
            // this is a slightly strange arrangement: since all state is really within a contained
            // view model (that only becomes available synchronously) we effectively assume we have
            // only our icicle state when that nested view model is not available.
            return if (navigationViewModel == null) {
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
        ({ callback: CallbackReceiver<NetworkResult<Experience>> -> graphQlApiService.fetchExperienceTask(ID(experienceId), campaignId.whenNotNull { ID(it) }, callback) }).asPublisher()

    private val epic: Observable<ExperienceViewModelInterface.Event> =
        Observable.merge(
            actions.map { action ->
                when (action) {
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
                        is NetworkResult.Error -> Observable.just(
                            ExperienceViewModelInterface.Event.DisplayError(
                                networkResult.throwable.message ?: "Unknown"
                            )
                        )
                        is NetworkResult.Success -> {

                            val navigationViewModel = resolveNavigationViewModel(
                                networkResult.response,
                                (state as State).navigationState
                            )

                            val experienceReadyEvent = ExperienceViewModelInterface.Event.ExperienceReady(navigationViewModel)

                            Observable.concat(
                                Observable.just(experienceReadyEvent),
                                Observable.merge(
                                    navigationViewModel.events.map { navigationEvent ->
                                        when (navigationEvent) {
                                            // pass the ViewEvents further up to the surrounding activity.
                                            // Any external navigation events (exit, load web URI, change backlight)
                                            // from the navigation view model need to be passed up.
                                            is ExperienceNavigationViewModelInterface.Emission.Event.NavigateAway -> {
                                                ExperienceViewModelInterface.Event.ExternalNavigation(navigationEvent.event)
                                            }
                                        }
                                    },
                                    navigationViewModel.updates.map { navigationViewUpdate ->
                                        when(navigationViewUpdate) {
                                            is ExperienceNavigationViewModelInterface.Emission.Update.SetBacklightBoost -> ExperienceViewModelInterface.Event.SetBacklightBoost(navigationViewUpdate.extraBright)
                                            is ExperienceNavigationViewModelInterface.Emission.Update.SetActionBar -> {
                                                ExperienceViewModelInterface.Event.SetActionBar(
                                                    navigationViewUpdate.experienceToolbarViewModel
                                                )
                                            }
                                            is ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen -> null // GoToScreen is an internal concern and not needed by us.
                                        }
                                    }
                                ).filterNulls()
                            ).doOnNext {
                                this@ExperienceViewModel.log.v("Observed navigation View Event: $it")
                            }
                        }
                    }
                }.doOnNext { event ->
                    if (event is ExperienceViewModelInterface.Event.ExperienceReady) {
                        log.v("Remembering experience navigation view model.")
                        navigationViewModel = event.experienceNavigationViewModel
                    }
                }
            ).share()

    override val events: Observable<ExperienceViewModelInterface.Event> = epic.shareAndReplayTypesOnResubscribe(
        // ExperienceReady should be replayed to any new subscriber to make sure they are brought up to date.
        ExperienceViewModelInterface.Event.ExperienceReady::class.java,
        ExperienceViewModelInterface.Event.SetBacklightBoost::class.java,
        ExperienceViewModelInterface.Event.SetActionBar::class.java
    )

    override fun pressBack() {
        if (navigationViewModel == null) {
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
    ) : Parcelable
}
