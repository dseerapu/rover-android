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
import io.rover.rover.core.streams.exactlyOnce
import io.rover.rover.core.streams.filter
import io.rover.rover.core.streams.filterNulls
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.core.streams.shareAndReplay
import io.rover.rover.core.streams.subscribe
import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity
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

    private val actions: PublishSubject<Action> = PublishSubject()

    private val screensById = experience.screens.associateBy { it.id.rawValue }

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
                if (activeScreenViewModel() == screen) {
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
        // the default starting state.  An empty backstack, which the reactive epic below
        // will populate with a an initial back stack frame for the home screen.
        State(
            listOf()
        )
    }
        protected set

    private fun activeScreenViewModel(): ScreenViewModelInterface {
        val currentScreenId = state.backStack.lastOrNull()?.screenId ?: throw RuntimeException("Backstack unexpectedly empty")
        return screenViewModelsById[currentScreenId] ?: throw RuntimeException("Unexpectedly found a dangling screen id in the back stack.")
    }

    /**
     * This method is responsible for defining the navigation behaviour that should occur
     * for all of the [Action]s.
     *
     * @return A [EmissionAndNewState], which includes what the entire new backstack and an emission.  The
     * event describes what the [ExperienceNavigationView] should do: transition to showing a new
     * ScreenView, open up an external web browser, or quit out completely.
     *
     * Override this if you would like to modify navigation behaviour (particularly, to launch an
     * external screen in your app, such as Login Screen), in response to the incoming Experience
     * Screen you are about to display having some characteristic, such as a custom key.  Be sure
     * to call `super` otherwise.
     *
     * Note: if you want to override behaviour when the navigation view is about to navigate to a
     * new Experience Screen, consider overriding [navigateToScreen] instead.
     */
    protected open fun actionBehaviour(currentBackStack: List<BackStackFrame>, action: Action): EmissionAndNewState? {
        val possiblePreviousScreenId = state.backStack.getOrNull(state.backStack.lastIndex - 1)?.screenId
        return when (action) {
            is Action.PressedBack -> {
                possiblePreviousScreenId.whenNotNull { previousScreenId ->
                    val screenViewModel = screenViewModelsById[previousScreenId]
                    val screen = screensById[previousScreenId]

                    when {
                        screenViewModel == null || screen == null -> {
                            log.w("Screen by id ${previousScreenId} missing from Experience with id ${experience.id.rawValue}.")
                            null
                        }
                        else -> navigateToScreen(screen, screenViewModel, currentBackStack, false)
                    }

                } ?: closeExperience(state.backStack) // can't go any further back: backstack would be empty; instead emit Exit.
            }
            is Action.PressedClose -> {
                closeExperience(state.backStack)
            }
            is Action.Navigate -> {
                when (action.navigateTo) {
                    is NavigateTo.OpenUrlAction -> EmissionAndNewState(
                        ExperienceNavigationViewModelInterface.Emission.Event.NavigateAway(
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
                            else -> navigateToScreen(screen, screenViewModel, currentBackStack, true)
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the needed [EmissionAndNewState] for navigating to a screen, backwards or forwards.
     *
     * You can override this to return a [ExperienceExternalNavigationEvent.Custom] event and thus
     * be able respond to it in your container (say, a subclass of
     * [StandaloneExperienceHostActivity]), and perform your custom behaviour, such as launching an
     * app login screen.
     *
     * @return an EventAndState which describes the [ExperienceNavigationViewModelInterface.Event]
     * event that should be emitted along with the new state of the backstack.
     */
    protected open fun navigateToScreen(
        screen: Screen,
        screenViewModel: ScreenViewModelInterface,
        currentBackStack: List<BackStackFrame>,
        forwards: Boolean
    ): EmissionAndNewState {
        return EmissionAndNewState(
            ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen(
                screenViewModel, !forwards, currentBackStack.isNotEmpty()
            ),
            if(forwards) {
                currentBackStack + listOf(BackStackFrame(screen.id.rawValue))
            } else {
                currentBackStack.subList(0, currentBackStack.lastIndex)
            }
        )
    }

    /**
     * Generates the needed [EmissionAndNewState] just for restoring the current Screen view model.
     *
     * Most users seeking to override navigation functionality should not need to override this method.
     */
    protected open fun restoreScreen(
        screenViewModel: ScreenViewModelInterface,
        currentBackStack: List<BackStackFrame>
    ): EmissionAndNewState {
        // just warp right to the current screen in the state
        return EmissionAndNewState(
            ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen(
                screenViewModel, false, false
            ),
            state.backStack
        )
    }

    /**
     * Generates the needed [EmissionAndNewState] to exit the Experience.
     *
     * You can override this to modify the exit behaviour, perhaps to return a
     * [ExperienceNavigationViewModelInterface.Emission.Event.NavigateAway] with a
     * [ExperienceExternalNavigationEvent.Custom] to inform your main Activity to instead perform no
     * effect at all.
     */
    protected open fun closeExperience(
        currentBackStack: List<BackStackFrame>
    ): EmissionAndNewState {
        return EmissionAndNewState(
            ExperienceNavigationViewModelInterface.Emission.Event.NavigateAway(ExperienceExternalNavigationEvent.Exit()),
            currentBackStack // no point changing the backstack: the view is just getting entirely popped
        )
    }

    /**
     * An [emission], which is an asynchronous event emitted from the view model which can either
     * update the UI or perhaps inform the containing components to perform an external navigation
     * operation.  The [newBackStack] is what the new back stack state should be after the event has
     * completed.
     */
    data class EmissionAndNewState(
        val emission: ExperienceNavigationViewModelInterface.Emission,
        val newBackStack: List<BackStackFrame>
    )

    /**
     * In response to navigating between screens, we may want to inject events for setting the
     * backlight boost or the toolbar.
     */
    private fun injectBehaviouralTransientEvents(
        event: ExperienceNavigationViewModelInterface.Emission
    ): Observable<ExperienceNavigationViewModelInterface.Emission> {
        // now take the event from the state change and inject some behavioural transient events
        // into the stream as needed (backlight and toolbar behaviours).
        // emit app bar update and BacklightBoost events (as needed) and into the stream for screen changes.
        val backlightEvent = when (event) {
            is ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen -> event.screenViewModel.needsBrightBacklight
            else -> null
        }.whenNotNull { ExperienceNavigationViewModelInterface.Emission.Update.SetBacklightBoost(it) }

        val appBarEvent = when (event) {
            is ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen -> event.screenViewModel.appBarConfiguration
            else -> null
        }.whenNotNull {
            val toolbarViewModel = viewModelFactory.viewModelForExperienceToolbar(it)
            ExperienceNavigationViewModelInterface.Emission.Update.SetActionBar(
                toolbarViewModel
            )
        }

        return Observable.concat(
            Observable.just(event),
            Observable.just(backlightEvent),
            Observable.just(appBarEvent)
        ).filterNulls()
    }

    // TODO: make this lazy to avoid constructor leakage warnings?
    private val epic = Observable.concat(
        Observable.just(
            if(state.backStack.isEmpty()) {
                // backstack is empty, so we're just starting out.  Navigate forward to the home screen in the experience!
                val homeScreen = screensById[experience.homeScreenId.rawValue] ?: throw RuntimeException("Home screen id is dangling.")
                val screenViewModel = screenViewModelsById[experience.homeScreenId.rawValue] ?: throw RuntimeException("Home screen id is dangling.")
                // so, in the case of nav view model. here are the following consumer points:

                navigateToScreen(
                    homeScreen,
                    screenViewModel,
                    state.backStack,
                    true
                )
            } else {
                restoreScreen(activeScreenViewModel(), state.backStack)
            }
        ),

        actions.map { action -> actionBehaviour(state.backStack, action) }
            .filterNulls()
    ).doOnNext { stateChange ->
        state = State(stateChange.newBackStack)
    }.flatMap { stateChange -> injectBehaviouralTransientEvents(stateChange.emission) }
    .doOnNext { event ->
        // side effects: we want to dispatch all events arriving from the toolbarviewmodel.
        // TODO: this subscriber may be moved to a separate field just to isolate it and improve clarity.
        if (event is ExperienceNavigationViewModelInterface.Emission.Update.SetActionBar) {
            event
                .experienceToolbarViewModel
                .toolbarEvents
                .subscribe { toolbarEvent ->
                    // subscribe to the events from the toolbar and dispatch them.
                    actions.onNext(
                        when (toolbarEvent) {
                            is ExperienceToolbarViewModelInterface.Event.PressedBack -> Action.PressedBack()
                            is ExperienceToolbarViewModelInterface.Event.PressedClose -> Action.PressedClose()
                        }
                    )
                }
        }
    }.shareAndReplay(10) // a count of 10 because only a couple events are emitted synchronously at startup that must be delivered to both events and updates.
        .doOnNext {
        log.v("Event: $it")
    } // only subscribers are events and updates, which both must see the entire stream.   I may be able to just set a max limit since both will subscribe quickly anyway.

    override val events: Observable<ExperienceNavigationViewModelInterface.Emission.Event> = epic
        .filter { emission -> emission is ExperienceNavigationViewModelInterface.Emission.Event }
        .map { it as ExperienceNavigationViewModelInterface.Emission.Event }
        .doOnNext { log.v("Event emitted.  Only should be one of these messages per event.") }
        .exactlyOnce()

        // TODO: I want to support ONLY ONE subscriber, and when nothing is subscribed, I want to
        // buffer all emissions, and emit them ONLY ONCE.  Except -- I do want multiple subscribers.
        //  What about the event service?

    override val updates: Observable<ExperienceNavigationViewModelInterface.Emission.Update> = epic
        .filter { emission -> emission is ExperienceNavigationViewModelInterface.Emission.Update }
        .map { it as ExperienceNavigationViewModelInterface.Emission.Update }
        // TODO: now I just need a "replay all unique types" operator!
        .shareAndReplayTypesOnResubscribe(
            // So, GoToScreen will retain its animation values.  it needs to be transformed.  However,
            // because in the event of a re-subscribe it is very likely that the
            // ExperienceNavigationView is new and has no current screen view, in which case it defaults
            // to no animation anyway.
            ExperienceNavigationViewModelInterface.Emission.Update.GoToScreen::class.java,
            ExperienceNavigationViewModelInterface.Emission.Update.SetActionBar::class.java,
            ExperienceNavigationViewModelInterface.Emission.Update.SetBacklightBoost::class.java
        )
        .doOnNext { log.v("Update emitted.  Only should be one two these messages per event.") }

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
