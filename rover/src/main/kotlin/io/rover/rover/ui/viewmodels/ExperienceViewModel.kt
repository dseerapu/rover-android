package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.Publisher
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.ViewModelFactoryInterface
import io.rover.rover.ui.types.NavigateTo
import kotlinx.android.parcel.Parcelize

/**
 * Behaviour for navigating through an experience.
 */
class ExperienceViewModel(
    val experience: Experience,
    val viewModelFactory: ViewModelFactoryInterface
): ExperienceViewModelInterface {
    // concerns:
    // start at home Screen.
    // maintain a backstack of ScreenView/ScreenViewModels.
    // persist state of which screen it's on.
    // have a canGoBack query method and a goBack() action creator, akin to WebView.

    // be aware that user extensions must be possible, but achieve it by writing a concise,
    // non-tightly coupled implementation and then think about extension points afterwards.

    // coming up with Events is a bit tricky: the View itself is going to want some degree
    // of state so it can do caching of views.  or maybe it can just cache by screen id, and
    // be gloriously ignorant of the actual stack? that's the ideal


    // in this case, we'll inject actions by subscribing to events coming from the viewmodels
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
        ScreenViewModel(it.value, viewModelFactory)

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
        .flatMap { it.events }
        // TODO: do the initial home screen warp in this chain somehow
        .subscribe( { item -> actions.onNext(Action.Navigate(item)) }, { error -> actions.onError(error) } )

    private sealed class Action {
        class PressedBack: Action()
        class Navigate(val navigateTo: NavigateTo): Action()
    }

    private var state = State(
        listOf(BackStackFrame(experience.homeScreenId.rawValue))
    )

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
                        ExperienceViewModelInterface.Event.GoBackwardToScreen(
                            screenViewModelsById[previousScreenId]!!
                        ),
                        // pop backstack:
                        state.backStack.subList(0, state.backStack.lastIndex)
                    )
                } ?: StateChange(
                    ExperienceViewModelInterface.Event.Exit(),
                    state.backStack // no change to backstack: the view is just getting entirely popped
                )
            }
            is Action.Navigate -> {
                when(action.navigateTo) {
                    is NavigateTo.OpenUrlAction -> StateChange(
                        ExperienceViewModelInterface.Event.OpenExternalWebBrowser(action.navigateTo.uri),
                        state.backStack // no change to backstack: the view is just getting entirely popped
                    )
                    is NavigateTo.GoToScreenAction -> StateChange(
                        ExperienceViewModelInterface.Event.GoForwardToScreen(
                            screenViewModelsById[action.navigateTo.screenId]!!
                        ),
                        state.backStack + listOf(BackStackFrame(action.navigateTo.screenId))
                    )
                }
            }
        }
    }

    data class StateChange(
        val event: ExperienceViewModelInterface.Event,
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
    override val events: Observable<ExperienceViewModelInterface.Event> = Publisher.concat(
        // emit a warp-to for all new subscribers so they are guaranteed to see their state.
        Publisher.just(ExperienceViewModelInterface.Event.WarpToScreen(activeScreen() ?: throw RuntimeException("Backstack unexpectedly empty"))),
        epic.share()
    )

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


