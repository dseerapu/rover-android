package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.ID
import io.rover.rover.services.network.NetworkResult
import io.rover.rover.services.network.NetworkServiceInterface
import io.rover.rover.streams.CallbackReceiver
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.Publisher
import io.rover.rover.streams.Subject
import io.rover.rover.streams.Subscriber
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.ViewModelFactoryInterface

class ExperienceViewModel(
    private val experienceId: String,
    private val networkService: NetworkServiceInterface,
    private val viewModelFactory: ViewModelFactoryInterface
): ExperienceViewModelInterface {



    // next I will need some state to track the "current" view model so I can emit BackPressed into it

    private var experienceViewModel : ExperienceNavigationViewModelInterface? = null

    private val epic : Observable<ExperienceViewModelInterface.Event> by lazy {
        actions.flatMap { action ->
            when(action) {
                Action.StartLoading -> ({ callback: CallbackReceiver<NetworkResult<Experience>> -> networkService.fetchExperienceTask(ID(experienceId), callback) }).asPublisher()
                .flatMap { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Error-> Observable.just(ExperienceViewModelInterface.Event.DisplayError(networkResult.throwable.message ?: "Unknown"))
                        is NetworkResult.Success -> Observable.concat(
                            Observable.just(
                                ExperienceViewModelInterface.Event.ExperienceReady(
                                    viewModelFactory.viewModelForExperience(networkResult.response)
                                )
                            ),
                            // Any external navigation events (exit, load web URI, change backlight)
                            // from the navigation view model need to be passed up.
                            // What will unsubscribe this when a new ExperienceNavigationViewModel
                            // comes through?  For now not likely to happen because this view model is not re-bound.
                            viewModelFactory.viewModelForExperience(networkResult.response).events.map { navigationEvent ->
                                when(navigationEvent) {
                                    is ExperienceNavigationViewModelInterface.Event.ViewEvent -> ExperienceViewModelInterface.Event.ViewEvent(navigationEvent.event)
                                    else -> null
                                }
                            }.filterNulls()
                        )
                    }
                }
            }
        }.map { event ->
            // side-effect (TODO should be doOnNext)
            if(event is ExperienceViewModelInterface.Event.ExperienceReady) {
                experienceViewModel = event.experienceNavigationViewModel
            }

            event
        }
    }

    private val sharedEpic = epic.share()

    override val events: Observable<ExperienceViewModelInterface.Event> = object : Publisher<ExperienceViewModelInterface.Event> by sharedEpic {
        // On being subscribed by a new subscriber I want to emit a StartLoading event.
        override fun subscribe(subscriber: Subscriber<ExperienceViewModelInterface.Event>) {
            // side-effect: start loading.
            actions.onNext(Action.StartLoading)
            sharedEpic.subscribe(subscriber)
        }
    }

    private val actions = PublishSubject<Action>()

    override fun pressBack() {
        // this isn't a great setup at the moment; delivering an event into a contained view model
        // (the experience navigation view model) that has been tracked locally as mutable state.
        // I can't quite think of a good pattern for this though.

        // oh crap. and because I require the icicle to make the view model... perhaps it really is the case that building/binding the
        experienceViewModel?.pressBack()
    }

    enum class Action {
        StartLoading
    }
}
