package io.rover.rover.plugins.userexperience.experience

import android.content.Context
import android.os.Parcelable
import android.view.WindowManager
import io.rover.rover.plugins.data.domain.Block
import io.rover.rover.plugins.data.domain.ButtonState
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.Row
import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.core.streams.Observable
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.concerns.BindableViewModel
import io.rover.rover.plugins.userexperience.experience.layout.row.RowViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceExternalNavigationEvent
import io.rover.rover.plugins.userexperience.experience.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.plugins.userexperience.experience.toolbar.ToolbarConfiguration
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationCenterListViewModelInterface

/**
 * Responsible for fetching and displaying an Experience, with the appropriate Android toolbar along
 * the top.
 */
interface ExperienceViewModelInterface : BindableViewModel {
    val events: Observable<Event>

    fun pressBack()

    sealed class Event {
        data class ExperienceReady(
            val experienceNavigationViewModel: ExperienceNavigationViewModelInterface
        ) : Event()

        data class SetActionBar(
            val toolbarViewModel: ExperienceToolbarViewModelInterface
        ) : Event()

        data class DisplayError(
            val message: String
        ) : Event()

        /**
         * This event signifies that the LayoutParams of the containing window should either be set
         * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
         */
        data class SetBacklightBoost(val extraBright: Boolean) : Event()

        /**
         * The user should be navigated somewhere external to
         */
        data class ExternalNavigation(
            val event: ExperienceExternalNavigationEvent
        ) : Event()
    }

    /**
     * Obtain a state object for this Experience View Model.
     *
     * This view model is the start of the chain of responsibility for any nested view models.
     */
    val state: Parcelable
}

/**
 * Construct and provide view model instances for the given parameters.  May implement singleton &
 * caching behaviour for some of them.
 *
 * You can use override or delegate to our implementation [StockViewModelFactory] in order to return
 * custom implementations of given view models in order to extend behaviour.
 *
 * This can be passed into constructors of view models so they can lazily create other view models,
 * particularly if such creation is data driven.
 */
interface ViewModelFactoryInterface {
    fun viewModelForExperienceNavigation(experience: Experience, icicle: Parcelable?): ExperienceNavigationViewModelInterface

    fun viewModelForExperience(experienceId: String, icicle: Parcelable?): ExperienceViewModelInterface

    fun viewModelForExperienceToolbar(toolbarConfiguration: ToolbarConfiguration): ExperienceToolbarViewModelInterface

    fun viewModelForNotificationCenter(context: Context): NotificationCenterListViewModelInterface
}
