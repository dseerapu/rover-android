package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.streams.Observable
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.ui.ViewModelFactoryInterface

class ButtonViewModel(
    private val block: ButtonBlock,
    private val blockViewModel: BlockViewModelInterface,
    private val viewModelFactory: ViewModelFactoryInterface
) : ButtonViewModelInterface {
    private val normalStateViewModel = viewModelFactory.viewModelForButtonState(block.normal)
    private val disabledStateViewModel = viewModelFactory.viewModelForButtonState(block.disabled)
    private val highlightedStateViewModel = viewModelFactory.viewModelForButtonState(block.highlighted)
    private val selectedStateViewModel = viewModelFactory.viewModelForButtonState(block.selected)

    private val epic = blockViewModel
        .events
        .map { event ->
            when(event) {
                is BlockViewModelInterface.Event.Touched -> ButtonViewModelInterface.Event.DisplayState(
                    highlightedStateViewModel, true, StateOfButton.Highlighted
                )
                is BlockViewModelInterface.Event.Released -> ButtonViewModelInterface.Event.DisplayState(
                    normalStateViewModel, true, StateOfButton.Normal
                )
                is BlockViewModelInterface.Event.Clicked -> ButtonViewModelInterface.Event.DisplayState(
                    selectedStateViewModel, true, StateOfButton.Selected
                )
            }
        }
        .share()

    override val buttonEvents = Observable.concat(
        // start by setting any newly subscribed view to the Normal state!
        Observable.just(
            ButtonViewModelInterface.Event.DisplayState(
                normalStateViewModel, false, StateOfButton.Normal
            )
        ),
        // and then subscribe to the event stream as normal
        epic
    )

    override fun viewModelForState(state: StateOfButton): ButtonStateViewModelInterface {
        return when(state) {
            StateOfButton.Selected -> selectedStateViewModel
            StateOfButton.Highlighted -> highlightedStateViewModel
            StateOfButton.Normal -> normalStateViewModel
            StateOfButton.Disabled -> disabledStateViewModel
        }
    }
}
