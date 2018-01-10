package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Parcelable
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.Publisher
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.ViewModelFactoryInterface
import io.rover.rover.ui.types.AppBarConfiguration
import io.rover.rover.ui.types.NavigateTo
import kotlinx.android.parcel.Parcelize

/**
 * Behaviour for navigating through an experience.
 *
 * Responsible for the following concerns: starting at the home screen, maintaining a backstack,
 * state persistence, WebView-like canGoBack/goBack methods, and exposing an API for customizing
 * flow behaviour.
 *
 * TODO: customization exposure.
 */
class ExperienceNavigationViewModel(
    private val experience: Experience,
    private val viewModelFactory: ViewModelFactoryInterface,
    private val toolbarViewModel: ExperienceToolbarViewModelInterface,
    icicle: Parcelable? = null
): ExperienceNavigationViewModelInterface, ExperienceToolbarViewModelInterface by toolbarViewModel {

    override fun pressBack() {
        actions.onNext(Action.PressedBack())
    }

    override fun canGoBack(): Boolean = state.backStack.size > 1

    // OK, so, I need the following cycle (in no particular start position):
    // Incoming button events/initial subscribe -> switch screen event for view -> which
    // events I subscribe to has to change

    private val actions : PublishSubject<Action> = PublishSubject()

    private val screensById = experience.screens.associateBy { it.id.rawValue }

    // TODO: right now we bring up viewmodels for the *entire* experience (ie., all the screens at
    // once).  This is unnecessary.
    private val screenViewModelsById: Map<String, ScreenViewModelInterface> = screensById.mapValues {
        // TODO: use DI to inject the screen view models
        viewModelFactory.viewModelForScreen(it.value)
    }

    /**
     * This subscriber listens to all the view models and then dispatches their navigation events
     * to our actions publisher.
     */
    private val screenEventSubscription = screenViewModelsById
        .values
        .asPublisher()
        .flatMap { screen ->
            screen.events.map { Pair(screen, it)}
        }
        .subscribe({ (screen, navigateTo) ->
            // filter out the the events that are not meant for the currently active screen:
            if(activeScreen() == screen) {
                actions.onNext(Action.Navigate(navigateTo))
            }
        }, { error -> actions.onError(error) })

    private sealed class Action {
        class PressedBack: Action()
        class Navigate(
            val navigateTo: NavigateTo
        ): Action()
    }

    override var state = if(icicle != null) {
        icicle as State
    } else {
        // the default starting state.  One stack frame, pointing to the experience screen set as
        // the "home" screen.
        State(
            listOf(BackStackFrame(experience.homeScreenId.rawValue))
        )
    }
        private set

    private fun activeScreen(): ScreenViewModelInterface {
        val currentScreenId = state.backStack.lastOrNull()?.screenId ?: throw RuntimeException("Backstack unexpectedly empty")
        return screenViewModelsById[currentScreenId] ?: throw RuntimeException("Unexpectedly found a dangling screen id in the back stack.")
    }

    init {
        toolbarViewModel.setConfiguration(activeScreen().appBarConfiguration)
    }

    // TODO: this is definitely going to be the custom behaviour injection opportunity
    private fun actionBehaviour(action: Action): StateChange? {
        val activeScreen = activeScreen()
        val possiblePreviousScreenId = state.backStack.getOrNull(state.backStack.lastIndex - 1)?.screenId
        return when(action) {
            is Action.PressedBack -> {
                possiblePreviousScreenId.whenNotNull { previousScreenId ->
                    StateChange(
                        ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen(
                            screenViewModelsById[previousScreenId]!!
                        ),
                        // pop backstack:
                        state.backStack.subList(0, state.backStack.lastIndex)
                    )
                } ?: StateChange(
                    ExperienceNavigationViewModelInterface.Event.ViewEvent(ExperienceViewEvent.Exit()),
                    state.backStack // no change to backstack: the view is just getting entirely popped
                )
            }
            is Action.Navigate -> {
                when(action.navigateTo) {
                    is NavigateTo.OpenUrlAction -> StateChange(
                        ExperienceNavigationViewModelInterface.Event.ViewEvent(
                            ExperienceViewEvent.OpenExternalWebBrowser(action.navigateTo.uri)
                        ),
                        state.backStack // no change to backstack: the view is just getting entirely popped
                    )
                    is NavigateTo.GoToScreenAction -> {
                        val viewModel = screenViewModelsById[action.navigateTo.screenId]
                            if(viewModel == null) {
                                log.w("Screen by id ${action.navigateTo.screenId} missing from Experience with id ${experience.id.rawValue}.")
                                null
                            } else {
                                StateChange(
                                    ExperienceNavigationViewModelInterface.Event.GoForwardToScreen(
                                        viewModel
                                    ),
                                    state.backStack + listOf(BackStackFrame(action.navigateTo.screenId))
                                )
                            }
                    }
                }
            }
        }
    }

    data class StateChange(
        val event: ExperienceNavigationViewModelInterface.Event,
        val newBackStack: List<BackStackFrame>
    )

    private fun injectBehaviouralTransientEvents(
        event: ExperienceNavigationViewModelInterface.Event
    ): Observable<ExperienceNavigationViewModelInterface.Event> {
        // now take the event from the state change and inject some behavioural transient events
        // into the stream as needed (backlight and toolbar behaviours).
        // emit app bar update and BacklightBoost events (as needed) and into the stream for screen changes.
        val backlightEvent = when(event) {
            is ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen -> event.screenViewModel.needsBrightBacklight
            is ExperienceNavigationViewModelInterface.Event.GoForwardToScreen -> event.screenViewModel.needsBrightBacklight
            is ExperienceNavigationViewModelInterface.Event.WarpToScreen -> event.screenViewModel.needsBrightBacklight
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.ViewEvent(ExperienceViewEvent.SetBacklightBoost(it)) }

        val appBarEvent = when(event) {
            is ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen -> event.screenViewModel.appBarConfiguration
            is ExperienceNavigationViewModelInterface.Event.GoForwardToScreen -> event.screenViewModel.appBarConfiguration
            is ExperienceNavigationViewModelInterface.Event.WarpToScreen -> event.screenViewModel.appBarConfiguration
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.SetActionBar(it) }

        return Observable.concat(
            Observable.just(event),
            Observable.just(backlightEvent),
            Observable.just(appBarEvent)
        ).filterNulls()
    }

    private val epic = actions
        .map { action -> actionBehaviour(action) }
        .filterNulls()
        .map { stateChange ->
            // abuse .map() for doOnNext() side-effects for now to update our state! TODO add doOnNext()
            state = State(stateChange.newBackStack)
            log.v("State change: $stateChange")

            stateChange
        }.flatMap { stateChange -> injectBehaviouralTransientEvents(stateChange.event)}
        .map { event ->
            // and to dispatch the change to the toolbar view model (TODO doOnNext)
            if (event is ExperienceNavigationViewModelInterface.Event.SetActionBar) {
                toolbarViewModel.setConfiguration(event.appBarConfiguration)
            }

            event
        }.share()

    override val events: Observable<ExperienceNavigationViewModelInterface.Event> = Publisher.concat(
        // emit a warp-to for all new subscribers so they are guaranteed to see their state.
        Publisher.just(
            ExperienceNavigationViewModelInterface.Event.WarpToScreen(
                activeScreen()
            )
        ).flatMap { event -> injectBehaviouralTransientEvents(event) },
        epic
    ).map { event ->
        log.v("Navigation view model event: $event")
        event
    }

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class BackStackFrame(
        val screenId: String
    ): Parcelable

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class State(
        val backStack: List<BackStackFrame>
    ): Parcelable
}


