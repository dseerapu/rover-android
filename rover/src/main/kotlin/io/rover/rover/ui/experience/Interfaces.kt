@file:JvmName("Interfaces")

package io.rover.rover.ui.experience

import android.os.Parcelable
import android.view.WindowManager
import io.rover.rover.core.domain.Block
import io.rover.rover.core.domain.ButtonState
import io.rover.rover.core.domain.Experience
import io.rover.rover.core.domain.Row
import io.rover.rover.core.domain.Screen
import io.rover.rover.streams.Observable
import io.rover.rover.ui.experience.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.concerns.BindableViewModel
import io.rover.rover.ui.experience.layout.row.RowViewModelInterface
import io.rover.rover.ui.experience.layout.screen.ScreenViewModelInterface
import io.rover.rover.ui.experience.navigation.ExperienceExternalNavigationEvent
import io.rover.rover.ui.experience.navigation.ExperienceNavigationViewModelInterface
import io.rover.rover.ui.experience.toolbar.ExperienceToolbarViewModelInterface
import io.rover.rover.ui.experience.toolbar.ToolbarConfiguration

/**
 * Responsible for fetching and displaying an Experience, with the appropriate Android toolbar along
 * the top.
 */
interface ExperienceViewModelInterface: BindableViewModel {
    val events: Observable<Event>

    fun pressBack()

    sealed class Event {
        data class ExperienceReady(
            val experienceNavigationViewModel: ExperienceNavigationViewModelInterface
        ): Event()

        data class SetActionBar(
            val toolbarViewModel: ExperienceToolbarViewModelInterface
        ): Event()

        data class DisplayError(
            val message: String
        ): Event()

        /**
         * This event signifies that the LayoutParams of the containing window should either be set
         * to either 1 or [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE].
         */
        data class SetBacklightBoost(val extraBright: Boolean): Event()

        /**
         * The user should be navigated somewhere external to
         */
        data class ExternalNavigation(
            val event: ExperienceExternalNavigationEvent
        ): Event()
    }

    /**
     * Obtain a state object for this Experience View Model.
     *
     * This view model is the start of the chain of responsibility for any nested view models.
     */
    val state: Parcelable
}

interface ViewModelFactoryInterface {
    fun viewModelForExperienceNavigation(experience: Experience, icicle: Parcelable?): ExperienceNavigationViewModelInterface

    fun viewModelForExperience(experienceId: String, icicle: Parcelable?): ExperienceViewModelInterface

    fun viewModelForExperienceToolbar(toolbarConfiguration: ToolbarConfiguration): ExperienceToolbarViewModelInterface

    fun viewModelForBlock(block: Block): BlockViewModelInterface

    fun viewModelForRow(row: Row): RowViewModelInterface

    fun viewModelForScreen(screen: Screen): ScreenViewModelInterface

    fun viewModelForButtonState(buttonState: ButtonState): ButtonStateViewModelInterface
}
