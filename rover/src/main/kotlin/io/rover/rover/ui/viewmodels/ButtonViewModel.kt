package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.BlockAction
import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.share
import io.rover.rover.ui.types.NavigateTo
import io.rover.rover.ui.types.RectF

class ButtonViewModel(
    private val block: ButtonBlock
) : ButtonViewModelInterface {
    override val text: String
        // TODO: state support is coming
        get() = block.normal.text

    private val eventSource = PublishSubject<ButtonViewModelInterface.Event>()
    override val events = eventSource.share()

    override fun click() {
        // I don't have an epic here, just a single event emitter, so I'll just publish an event
        // immediately.

        log.v("CLICKED. ${block.action}")
        val navigateTo = when(block.action) {
            null -> null
            is BlockAction.GoToScreenAction -> { NavigateTo.GoToScreenAction(block.action.screenId.rawValue) }
            is BlockAction.OpenUrlAction -> { NavigateTo.OpenUrlAction(block.action.url)}
        }

        navigateTo.whenNotNull {
            eventSource.onNext(
                ButtonViewModelInterface.Event.Clicked(it)
            )
        }
    }
}
