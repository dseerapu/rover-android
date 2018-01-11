package io.rover.rover.ui.viewmodels

import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.streams.shareAndReplayTypesOnResubscribe
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.types.ToolbarConfiguration

class ExperienceToolbarViewModel(): ExperienceToolbarViewModelInterface {
    private var toolbarConfiguration: ToolbarConfiguration? = null

    override fun setConfiguration(toolbarConfiguration: ToolbarConfiguration) {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.SetToolbar(toolbarConfiguration))
    }

    override fun pressedBack() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedBack())
    }

    override fun pressedClose() {
        actions.onNext(ExperienceToolbarViewModelInterface.Event.PressedClose())
    }

    private val actions = PublishSubject<ExperienceToolbarViewModelInterface.Event>()

    override val toolbarEvents: Observable<ExperienceToolbarViewModelInterface.Event> = Observable.concat(
        Observable.defer { Observable.just(toolbarConfiguration) } .map {
            it
        }.filterNulls().map { configuration -> ExperienceToolbarViewModelInterface.Event.SetToolbar(configuration) },
        actions.share()
    ).shareAndReplayTypesOnResubscribe(
        // re-emit SetToolbar to any subsequent subscribers
        ExperienceToolbarViewModelInterface.Event.SetToolbar::class.java
    )

    /**
     * Subscribe to our own events stream to keep local state up to date.
     */
    private val stateUpdateSubscriber = toolbarEvents.subscribe { event ->
        when(event) {
            is ExperienceToolbarViewModelInterface.Event.SetToolbar -> this.toolbarConfiguration = event.toolbarConfiguration
        }
    }
}
