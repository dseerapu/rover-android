package io.rover.rover.ui.experience.toolbar

import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.share
import io.rover.rover.ui.types.ToolbarConfiguration
import io.rover.rover.ui.viewmodels.ExperienceToolbarViewModelInterface

class ExperienceToolbarViewModel(
    override val configuration: ToolbarConfiguration
): ExperienceToolbarViewModelInterface {

    override fun pressedBack() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedBack())
    }

    override fun pressedClose() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedClose())
    }

    private val actions = PublishSubject<ExperienceToolbarViewModelInterface.Event>()

    override val toolbarEvents = actions.share()
}
