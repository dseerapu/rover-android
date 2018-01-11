package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.asPublisher
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.flatMap
import io.rover.rover.streams.map
import io.rover.rover.streams.shareAndReplayTypesOnResubscribe
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
        /**
         * Emitted into the actions stream if the close button is pressed on the toolbar.
         */
        class PressedClose : Action()
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

    // TODO: this is definitely going to be the custom behaviour injection opportunity
    private fun actionBehaviour(action: Action): StateChange? {
        val activeScreen = activeScreen()
        val possiblePreviousScreenId = state.backStack.getOrNull(state.backStack.lastIndex - 1)?.screenId
        return when(action) {
            is Action.PressedBack -> {
                possiblePreviousScreenId.whenNotNull { previousScreenId ->
                    StateChange(
                        ExperienceNavigationViewModelInterface.Event.GoToScreen(
                            screenViewModelsById[previousScreenId]!!,
                            true,
                            true
                        ),
                        // pop backstack:
                        state.backStack.subList(0, state.backStack.lastIndex)
                    )
                } ?: StateChange(
                    ExperienceNavigationViewModelInterface.Event.ViewEvent(ExperienceExternalNavigationEvent.Exit()),
                    state.backStack // no change to backstack: the view is just getting entirely popped
                )
            }
            is Action.PressedClose -> {
                StateChange(
                    ExperienceNavigationViewModelInterface.Event.ViewEvent(ExperienceExternalNavigationEvent.Exit()),
                    state.backStack // no change to backstack: the view is just getting entirely popped
                )
            }
            is Action.Navigate -> {
                when(action.navigateTo) {
                    is NavigateTo.OpenUrlAction -> StateChange(
                        ExperienceNavigationViewModelInterface.Event.ViewEvent(
                            ExperienceExternalNavigationEvent.OpenExternalWebBrowser(action.navigateTo.uri)
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
                                    ExperienceNavigationViewModelInterface.Event.GoToScreen(
                                        viewModel, false, true
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
            is ExperienceNavigationViewModelInterface.Event.GoToScreen -> event.screenViewModel.needsBrightBacklight
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.SetBacklightBoost(it) }

        val appBarEvent = when(event) {
            is ExperienceNavigationViewModelInterface.Event.GoToScreen -> event.screenViewModel.appBarConfiguration
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.SetActionBar(it) }

        return Observable.concat(
            Observable.just(event),
            Observable.just(backlightEvent),
            Observable.just(appBarEvent)
        ).filterNulls()
    }

    private val epic = Observable.concat(
        Observable.just(
            // just warp right to the current screen in the state (or the home screen in the
            // experience).
            StateChange(
                ExperienceNavigationViewModelInterface.Event.GoToScreen(
                    activeScreen(), false, false
                ),
                state.backStack
            )
        ),

        Observable.merge(
            actions,
            toolbarViewModel.toolbarEvents.map { toolbarEvent ->
                // the toolbar has buttons that can emit Back or Close events depending on the
                // button configuration.
                when(toolbarEvent) {
                    is ExperienceToolbarViewModelInterface.Event.PressedClose -> Action.PressedClose()
                    is ExperienceToolbarViewModelInterface.Event.PressedBack -> Action.PressedBack()
                    is ExperienceToolbarViewModelInterface.Event.SetToolbar -> null
                }
            }.filterNulls()
        ).map { action -> actionBehaviour(action) }
        .filterNulls()
    ).map { stateChange ->
        // abuse .map() for doOnNext() side-effects for now to update our state! TODO add doOnNext()
        state = State(stateChange.newBackStack)
        stateChange
    }.flatMap { stateChange -> injectBehaviouralTransientEvents(stateChange.event) }
    .map { event ->
        // and to dispatch the change to the toolbar view model (TODO doOnNext)
        if (event is ExperienceNavigationViewModelInterface.Event.SetActionBar) {
            log.v("Setting action bar: ${event.appBarConfiguration}")
            toolbarViewModel.setConfiguration(event.appBarConfiguration)
        }

        event
    }.map {
        log.v("Event: $it")
        it
    }.share()


    override val events: Observable<ExperienceNavigationViewModelInterface.Event> = epic.shareAndReplayTypesOnResubscribe(
        // TODO oh shit.  GoToScreen would retain its animation values.  it needs to be transformed.
        ExperienceNavigationViewModelInterface.Event.GoToScreen::class.java,
        ExperienceNavigationViewModelInterface.Event.SetActionBar::class.java,
        ExperienceNavigationViewModelInterface.Event.SetBacklightBoost::class.java
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


