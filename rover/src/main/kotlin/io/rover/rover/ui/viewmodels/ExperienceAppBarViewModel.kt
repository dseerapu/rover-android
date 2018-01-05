package io.rover.rover.ui.viewmodels

import io.rover.rover.streams.Observable
import io.rover.rover.streams.filterNulls
import io.rover.rover.streams.map
import io.rover.rover.streams.share

/**
 * Control the Activity's app bar from
 */
class ExperienceAppBarViewModel(
    val experienceViewModel: ExperienceViewModelInterface
): ExperienceAppBarViewModelInterface {
    private val epic = experienceViewModel
        .events
        .map { event ->
            when(event) {
                is ExperienceViewModelInterface.Event.ViewEvent -> {
                    when(event.event) {
                        is ExperienceViewEvent.SetActionBar -> {
                            ExperienceAppBarViewModelInterface.Event(
                                event.event.appBarConfiguration
                            )
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }.filterNulls()

    override val events: Observable<ExperienceAppBarViewModelInterface.Event> = epic.share()
}
