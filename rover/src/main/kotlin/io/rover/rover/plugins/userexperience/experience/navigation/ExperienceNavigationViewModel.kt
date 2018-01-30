package io.rover.rover.plugins.userexperience.experience.navigation

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.streams.doOnNext
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.core.streams.share
import io.rover.rover.core.streams.subscribe
import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface
import kotlinx.android.parcel.Parcelize

/**
 * Behaviour for navigating through an experience.
 *
 * Responsible for the following concerns: starting at the home screen, maintaining a backstack,
 * state persistence, WebView-like canGoBack/goBack methods, and exposing an API for customizing
 * flow behaviour.
 */
open class ExperienceNavigationViewModel(
    private val experience: Experience,
    private val blockViewModelFactory: BlockViewModelFactoryInterface,
    private val viewModelFactory: ViewModelFactoryInterface,
    // TODO: consider an optional interface type here called "CustomNavigationBehaviour", which implementers may provide if they want custom nav
    icicle: Parcelable? = null
) : ExperienceNavigationViewModelInterface {

    override fun pressBack() {
        actions.onNext(Action.PressedBack())
    }

    override fun canGoBack(): Boolean = state.backStack.size > 1

    // OK, so, I need the following cycle (in no particular start position):
    // Incoming button events/initial subscribe -> switch screen event for view -> which
    // events I subscribe to has to change

    private val actions: PublishSubject<Action> = PublishSubject()

    protected val screensById = experience.screens.associateBy { it.id.rawValue }

    // TODO: right now we bring up viewmodels for the *entire* experience (ie., all the screens at
    // once).  This is unnecessary.
    private val screenViewModelsById: Map<String, ScreenViewModelInterface> = screensById.mapValues {
        // TODO: this should be lazy instead! which means I also need to change how the event
        // subscription below works
        blockViewModelFactory.viewModelForScreen(it.value)
    }

    init {
        /**
         * This subscriber listens to all the view models and then dispatches their navigation events
         * to our actions publisher.
         */
        screenViewModelsById
            .values
            .asPublisher()
            .flatMap { screen ->
                screen.events.map { Pair(screen, it) }
            }
            .subscribe({ (screen, navigateTo) ->
                // filter out the the events that are not meant for the currently active screen:
                if (activeScreen() == screen) {
                    actions.onNext(Action.Navigate(navigateTo))
                }
            }, { error -> actions.onError(error) })
    }

    protected sealed class Action {
        class PressedBack : Action()
        /**
         * Emitted into the actions stream if the close button is pressed on the toolbar.
         */
        class PressedClose : Action()
        class Navigate(
            val navigateTo: NavigateTo
        ) : Action()
    }

    override var state = if (icicle != null) {
        icicle as State
    } else {
        // the default starting state.  One stack frame, pointing to the experience screen set as
        // the "home" screen.
        State(
            listOf(BackStackFrame(experience.homeScreenId.rawValue))
        )
    }
        protected set

    private fun activeScreen(): ScreenViewModelInterface {
        val currentScreenId = state.backStack.lastOrNull()?.screenId ?: throw RuntimeException("Backstack unexpectedly empty")
        return screenViewModelsById[currentScreenId] ?: throw RuntimeException("Unexpectedly found a dangling screen id in the back stack.")
    }

    /**
     * This method is responsible for defining the navigation behaviour that should occur
     * for all of the [Action]s.
     *
     * @return A [EventAndNewState], which includes what the entire new backstack and an event.  The
     * event describes what the [ExperienceNavigationView] should do: transition to showing a new
     * ScreenView, open up an external web browser, or quit out completely.
     *
     * Override this if you would like to modify navigation behaviour (particularly, to launch an
     * external screen in your app, such as Login Screen), in response to the incoming Experience
     * Screen you are about to display having some characteristic, such as a meta-property.  Be sure
     * to call `super` otherwise.
     *
     * Note: if you want to override behaviour when the navigation view is about to navigate to a
     * new Experience Screen, consider overriding [navigateForwardToScreen] instead.
     */
    protected open fun actionBehaviour(currentBackStack: List<BackStackFrame>, action: Action): EventAndNewState? {
        val possiblePreviousScreenId = state.backStack.getOrNull(state.backStack.lastIndex - 1)?.screenId
        return when (action) {
            is Action.PressedBack -> {
                possiblePreviousScreenId.whenNotNull { previousScreenId ->
                    EventAndNewState(
                        ExperienceNavigationViewModelInterface.Event.GoToScreen(
                            screenViewModelsById[previousScreenId]!!,
                            true,
                            true
                        ),
                        // pop backstack:
                        state.backStack.subList(0, state.backStack.lastIndex)
                    )
                } ?: EventAndNewState(
                    // backstack would be empty; instead emit Exit.
                    ExperienceNavigationViewModelInterface.Event.NavigateAway(ExperienceExternalNavigationEvent.Exit()),
                    state.backStack // no point changing the backstack: the view is just getting entirely popped
                )
            }
            is Action.PressedClose -> {
                EventAndNewState(
                    ExperienceNavigationViewModelInterface.Event.NavigateAway(ExperienceExternalNavigationEvent.Exit()),
                    state.backStack // no point changing the backstack: the view is just getting entirely popped
                )
            }
            is Action.Navigate -> {
                when (action.navigateTo) {
                    is NavigateTo.OpenUrlAction -> EventAndNewState(
                        ExperienceNavigationViewModelInterface.Event.NavigateAway(
                            ExperienceExternalNavigationEvent.OpenExternalWebBrowser(action.navigateTo.uri)
                        ),
                        // no change to backstack: something will instead be pushed onto the
                        // containing backstack (ie., the Android one or perhaps a custom one used
                        // by the app) rather than this one.
                        state.backStack
                    )
                    is NavigateTo.GoToScreenAction -> {
                        val screenViewModel = screenViewModelsById[action.navigateTo.screenId]
                        val screen = screensById[action.navigateTo.screenId]

                        return when {
                            screenViewModel == null || screen == null -> {
                                log.w("Screen by id ${action.navigateTo.screenId} missing from Experience with id ${experience.id.rawValue}.")
                                null
                            }
                            else -> navigateForwardToScreen(screen, screenViewModel)
                        }
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @return an EventAndState which describes the [ExperienceNavigationViewModelInterface.Event]
     * event that should be emitted.
     *
     * If you are intending to override this to launch your own custom app behaviour in response to
     * Screens having a certain characteristic
     */
    open protected fun navigateForwardToScreen(
        screen: Screen,
        screenViewModel: ScreenViewModelInterface
    ): EventAndNewState {
        return EventAndNewState(
            ExperienceNavigationViewModelInterface.Event.GoToScreen(
                screenViewModel, false, true
            ),
            state.backStack + listOf(BackStackFrame(screen.id.rawValue))
        )
    }

    data class EventAndNewState(
        val event: ExperienceNavigationViewModelInterface.Event,
        val newBackStack: List<BackStackFrame>
    )

    /**
     * In response to navigating between screens, we may want to inject events for setting the
     * backlight boost or the toolbar.
     */
    private fun injectBehaviouralTransientEvents(
        event: ExperienceNavigationViewModelInterface.Event
    ): Observable<ExperienceNavigationViewModelInterface.Event> {
        // now take the event from the state change and inject some behavioural transient events
        // into the stream as needed (backlight and toolbar behaviours).
        // emit app bar update and BacklightBoost events (as needed) and into the stream for screen changes.
        val backlightEvent = when (event) {
            is ExperienceNavigationViewModelInterface.Event.GoToScreen -> event.screenViewModel.needsBrightBacklight
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Event.SetBacklightBoost(it) }

        val appBarEvent = when (event) {
            is ExperienceNavigationViewModelInterface.Event.GoToScreen -> event.screenViewModel.appBarConfiguration
            else -> null
        }.whenNotNull {
            val toolbarViewModel = viewModelFactory.viewModelForExperienceToolbar(it)
            ExperienceNavigationViewModelInterface.Event.SetActionBar(
                toolbarViewModel
            )
        }

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
            EventAndNewState(
                ExperienceNavigationViewModelInterface.Event.GoToScreen(
                    activeScreen(), false, false
                ),
                state.backStack
            )
        ),

        actions.map { action -> actionBehaviour(state.backStack, action) }
            .filterNulls()
    ).doOnNext { stateChange ->
        state = State(stateChange.newBackStack)
    }.flatMap { stateChange -> injectBehaviouralTransientEvents(stateChange.event) }
    .doOnNext { event ->
        if (event is ExperienceNavigationViewModelInterface.Event.SetActionBar) {
            event
                .experienceToolbarViewModel
                .toolbarEvents
                .subscribe { toolbarEvent ->
                    // subscribe to the events from the toolbar and dispatch them
                    actions.onNext(
                        when (toolbarEvent) {
                            is ExperienceToolbarViewModelInterface.Event.PressedBack -> Action.PressedBack()
                            is ExperienceToolbarViewModelInterface.Event.PressedClose -> Action.PressedClose()
                        }
                    )
                }
        }
    }.doOnNext {
        log.v("Event: $it")
    }.share()

    override val events: Observable<ExperienceNavigationViewModelInterface.Event> = epic.shareAndReplayTypesOnResubscribe(
        // So, GoToScreen will retain its animation values.  it needs to be transformed.  However,
        // because in the event of a re-subscribe it is very likely that the
        // ExperienceNavigationView is new and has no current screen view, in which case it defaults
        // to no animation anyway.
        ExperienceNavigationViewModelInterface.Event.GoToScreen::class.java,
        ExperienceNavigationViewModelInterface.Event.SetActionBar::class.java,
        ExperienceNavigationViewModelInterface.Event.SetBacklightBoost::class.java
    )

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class BackStackFrame(
        val screenId: String
    ) : Parcelable

    // @Parcelize Kotlin synthetics are generating the CREATOR method for us.
    @SuppressLint("ParcelCreator")
    @Parcelize
    data class State(
        val backStack: List<BackStackFrame>
    ) : Parcelable
}
