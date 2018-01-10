package io.rover.rover.ui.viewmodels

import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.map
import io.rover.rover.streams.shareHotAndReplay
import io.rover.rover.ui.types.AppBarConfiguration

class ExperienceToolbarViewModel(): ExperienceToolbarViewModelInterface {
    override fun setConfiguration(toolbarConfiguration: AppBarConfiguration) {
        actions.onNext(toolbarConfiguration)
    }

    private val actions = PublishSubject<AppBarConfiguration>()

    override val toolbarEvents: Observable<ExperienceToolbarViewModelInterface.Event> = actions.map {
        ExperienceToolbarViewModelInterface.Event(it)
    }.shareHotAndReplay(1) // we want to share hot and replay in order to capture any events that occur before any subscribers arrive
}
