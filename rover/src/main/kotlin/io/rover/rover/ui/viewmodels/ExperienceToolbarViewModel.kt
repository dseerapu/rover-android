package io.rover.rover.ui.viewmodels

import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.types.ToolbarConfiguration

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
