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
    val experience: Experience,
    val viewModelFactory: ViewModelFactoryInterface,
    val icicle: Parcelable? = null
): ExperienceNavigationViewModelInterface {
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

        // TODO: I need to subscribe to all of these, and then when done, I need to emit a
        // single WarpTo event for the home Screen. Even
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
        State(
            listOf(BackStackFrame(experience.homeScreenId.rawValue))
        )
    }
        private set

    private fun activeScreen(): ScreenViewModelInterface? {
        val currentScreenId = state.backStack.lastOrNull()?.screenId
        return currentScreenId.whenNotNull { screenViewModelsById[it] }
    }

    // TODO: this is definitely going to be the custom behaviour injection opportunity
    private fun actionBehaviour(action: Action): StateChange {
        val activeScreen = activeScreen()
        val possiblePreviousScreenId = state.backStack.getOrNull(state.backStack.lastIndex - 1)?.screenId
        return when(action) {
            is Action.PressedBack -> {
                possiblePreviousScreenId.whenNotNull { previousScreenId ->
                    StateChange(
                        ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen(
                            screenViewModelsById[previousScreenId]!!,
                            ExperienceNavigationViewModelInterface.AppBarState(Color.RED)
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
                    is NavigateTo.GoToScreenAction -> StateChange(
                        ExperienceNavigationViewModelInterface.Event.GoForwardToScreen(
                            screenViewModelsById[action.navigateTo.screenId] ?: throw RuntimeException("Screen by id ${action.navigateTo.screenId} missing from Experience with id ${experience.id.rawValue}."),
                            ExperienceNavigationViewModelInterface.AppBarState(Color.RED)
                        ),
                        state.backStack + listOf(BackStackFrame(action.navigateTo.screenId))
                    )
                }
            }
        }
    }

    data class StateChange(
        val event: ExperienceNavigationViewModelInterface.Event,
        val newBackStack: List<BackStackFrame>
    )

    private val epic = actions
        .map { action -> actionBehaviour(action) }
        .map { stateChange ->
            // abuse .map() for doOnNext() side-effects for now to update our state! TODO add doOnNext()
            state = State(stateChange.newBackStack)
            log.v("State change: $stateChange")
            stateChange
        }.map { stateChange -> stateChange.event }


    // TODO: onSubscribe we want to emit the WarpTo event.  I need a concat transform to do that tho
    override val events: Observable<ExperienceNavigationViewModelInterface.Event> = Publisher.concat(
        // emit a warp-to for all new subscribers so they are guaranteed to see their state.
        Publisher.just(
            ExperienceNavigationViewModelInterface.Event.WarpToScreen(
                activeScreen() ?: throw RuntimeException("Backstack unexpectedly empty"),
                ExperienceNavigationViewModelInterface.AppBarState(Color.RED)
            )
        ),
        epic.share()
    ).flatMap { event ->
        // emit an additional BacklightBoost event into the stream for screen changes.
        val backlightEvent = when(event) {
            is ExperienceNavigationViewModelInterface.Event.GoBackwardToScreen -> event.screenViewModel.needsBrightBacklight
            is ExperienceNavigationViewModelInterface.Event.GoForwardToScreen -> event.screenViewModel.needsBrightBacklight
            is ExperienceNavigationViewModelInterface.Event.WarpToScreen -> event.screenViewModel.needsBrightBacklight
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.ViewEvent(ExperienceViewEvent.SetBacklightBoost(it)) }

        Observable.concat(
            Observable.just(event),
            Observable.just(backlightEvent)
        ).filterNulls()
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


