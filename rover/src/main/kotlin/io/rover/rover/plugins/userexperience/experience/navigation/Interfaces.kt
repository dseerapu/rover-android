package io.rover.rover.plugins.userexperience.experience.navigation

import android.os.Parcelable
import io.rover.rover.core.streams.Observable
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface
import java.net.URI

interface ExperienceNavigationViewModelInterface : BindableViewModel {
    val events: Observable<Event>

    fun pressBack()

    /**
     * Ask the view model if there are any entries on its internal back stack to revert to.
     *
     * Check this before calling [pressBack].  However, it is optional: if you call pressBack()
     * without checking [canGoBack], and there are no remaining back stack entries remaining, you'll
     * receive an [ExperienceNavigationViewModelInterface.Event.Exit] event.
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

    sealed class Event {
        data class GoToScreen(
            val screenViewModel: ScreenViewModelInterface,
            val backwards: Boolean,
            val animate: Boolean
        ) : Event()

        data class ViewEvent(
            val event: ExperienceExternalNavigationEvent
        ) : Event()

        data class SetActionBar(
            val experienceToolbarViewModel: ExperienceToolbarViewModelInterface
        ) : Event()

        /**
         * This event signifies that the LayoutParams of the containing window should either be set
         * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
         */
        data class SetBacklightBoost(val extraBright: Boolean) : Event()
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
}