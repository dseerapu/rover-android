package io.rover.rover.plugins.userexperience.experience.toolbar

import io.rover.rover.core.streams.PublishSubject
import io.rover.rover.core.streams.share

class ExperienceToolbarViewModel(
    override val configuration: ToolbarConfiguration
) : ExperienceToolbarViewModelInterface {

    override fun pressedBack() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedBack())
    }

    override fun pressedClose() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedClose())
    }

    private val actions = PublishSubject<ExperienceToolbarViewModelInterface.Event>()

    override val toolbarEvents = actions.share()
}
