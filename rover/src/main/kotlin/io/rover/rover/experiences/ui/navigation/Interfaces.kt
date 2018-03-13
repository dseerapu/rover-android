package io.rover.rover.plugins.userexperience.experience.navigation

import android.app.Activity
import android.os.Parcelable
import android.view.WindowManager
import io.rover.rover.core.streams.Observable
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModel
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModel
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface
import java.net.URI

interface ExperienceNavigationViewModelInterface : BindableViewModel {
    /**
     * The containing view model can subscribe to be informed of user input that it needs to act on.
     *
     * May only have one subscriber.
     */
    val events: Observable<Emission.Event>

    /**
     * The View should subscribe to this stream and thus be kept up to date.
     *
     * In this case events include instructions to navigate to and display an Experience screen and
     * set the backlight and toolbar.
     *
     * When newly subscribed, the view will immediately receive such events as are necessary
     * to fully populate it.
     */
    val updates: Observable<Emission.Update>

    fun pressBack()

    /**
     * Ask the view model if there are any entries on its internal back stack to revert to.
     *
     * Check this before calling [pressBack].  However, it is optional: if you call pressBack()
     * without checking [canGoBack], and there are no remaining back stack entries remaining, you'll
     * receive an [ExperienceNavigationViewModelInterface.Event.NavigateAway] containing a
     * [ExperienceExternalNavigationEvent.Exit] event.
     */
    fun canGoBack(): Boolean

    /**
     * Obtain a state object for this Experience Navigation View Model.
     *
     * TODO: determine if in fact it is worth exposing my own State interface type here (but not the
     * full type to avoid exposing internals).  A bit more boilerplate but it allows consuming view
     * models (that contain this one) to have stronger type guarantees in their own state bundles.
     */
    val state: Parcelable

    /**
     * All items that can be asynchronously emitted to subscribers of this view model.  It is split
     * into two categories: Event, and Update.
     *
     * They both have somewhat different semantics.  Events are only valid in the same "moment" at
     * which they are emitted, but equally require guranteed, single delivery to their subscriber.
     *
     * Updates, on the other hand, are idempotent and may be re-emitted to bring new subscribers up
     * to date.
     */
    sealed class Emission {
        /**
         * The subscribing view model will receive these.  See [updates].
         */
        sealed class Event: Emission() {
            data class NavigateAway(
                val event: ExperienceExternalNavigationEvent
            ) : Event()
        }

        /**
         * The View should subscribe to these and thus be kept up to date.  See [events].
         */
        sealed class Update: Emission() {
            data class GoToScreen(
                val screenViewModel: ScreenViewModelInterface,
                val backwards: Boolean,
                val animate: Boolean
            ) : Update()

            data class SetActionBar(
                val experienceToolbarViewModel: ExperienceToolbarViewModelInterface
            ) : Update()

            /**
             * This event signifies that the brightness parameter LayoutParams of the containing window
             * should either be set to either 1 or
             * [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
             */
            data class SetBacklightBoost(val extraBright: Boolean) : Update()
        }
    }
}

/**
 * These are navigation that are emitted by the experience navigation view model but because they
 * are for destinations external to the experience they must be passed up by the containing
 * ExperienceViewModel.
 */
sealed class ExperienceExternalNavigationEvent {
    // TODO: we may want to do an (optional) internal web browser like iOS, but there is less call for it
    // because Android has its back button.  Will discuss.

    // TODO: add an Event here for customers to insert a custom navigation event that their own code
    // can handle on the outer side of ExperienceViewModel for navigating to other screens in their
    // app and such.

    /**
     *  Containing view context should launch a web browser for the given URI in the surrounding
     *  navigation flow (such as the general Android backstack, Conductor backstack, etc.) external
     *  to the internal Rover ExperienceNavigationViewModel, whatever it happens to be in the
     *  surrounding app.
     */
    data class OpenExternalWebBrowser(val uri: URI) : ExperienceExternalNavigationEvent()

    /**
     * Containing view context (hosting the Experience) should pop itself ([Activity.finish], etc.)
     * in the surrounding navigation flow (such as the general Android backstack, Conductor
     * backstack, etc.) external to the internal Rover ExperienceNavigationViewModel, whatever it
     * happens to be in the surrounding app.
     */
    class Exit : ExperienceExternalNavigationEvent()

    /**
     * This is a custom navigation type.  It is not used in typical operation of the Rover SDK's
     * Experiences plugin, however, to insert custom behaviour developers may override
     * [ExperienceNavigationViewModel], [ScreenViewModel], or [BlockViewModel] to emit these Custom
     * events thus handle them in their [ExperienceView] container.  A common use case is to peek at
     * incoming screens that have some sort of "needs login" meta property within
     * [ExperienceNavigationViewModel], emit a custom event, and then consuming it within a custom
     * [ExperienceView] to launch your native, non-Rover, login screen.
     *
     * See the documentation for further details.
     */
    data class Custom(val uri: String): ExperienceExternalNavigationEvent()
}
