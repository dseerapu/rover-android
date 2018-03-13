package io.rover.rover.plugins.userexperience.experience.toolbar

import android.support.v7.widget.Toolbar
import io.rover.rover.core.streams.Observable

interface ViewExperienceAppBarInterface {
    var experienceAppBarViewModel: ExperienceAppBarViewModelInterface?
}

interface ViewExperienceToolbarInterface {
    /**
     * Set the toolbar view model.  However, uncharacteristically of the other bindable view mixins,
     * this one is a method that returns a new [Toolbar] view.  This must be done because
     * Android's Toolbar has limitations that prevent all of its styling being configurable after
     * creation. Thus, uncharacteristically of the View mixins, this one is responsible for actually
     * creating the view.
     */
    fun setViewModelAndReturnToolbar(
        toolbarViewModel: ExperienceToolbarViewModelInterface
    ): Toolbar
}

interface ExperienceAppBarViewModelInterface {
    val events: Observable<Event>

    class Event(
        val appBarConfiguration: ToolbarConfiguration
    )
}

interface ExperienceToolbarViewModelInterface {
    val toolbarEvents: Observable<Event>

    val configuration: ToolbarConfiguration

    fun pressedBack()

    fun pressedClose()

    sealed class Event {
        class PressedBack : Event()
        class PressedClose : Event()
    }
}