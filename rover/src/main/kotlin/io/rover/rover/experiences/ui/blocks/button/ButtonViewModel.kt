package io.rover.rover.experiences.ui.blocks.button

import io.rover.rover.core.data.domain.ButtonBlock
import io.rover.rover.core.data.domain.ButtonState
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.map
import io.rover.rover.core.streams.share
import io.rover.rover.experiences.ui.blocks.BlockViewModelFactoryInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface

class ButtonViewModel(
    block: ButtonBlock,
    blockViewModel: BlockViewModelInterface,
    // viewModelFactory: BlockViewModelFactoryInterface
    stateViewModelResolver: (buttonState: ButtonState) -> ButtonStateViewModelInterface
) : ButtonViewModelInterface {
    private val normalStateViewModel = stateViewModelResolver(block.normal)
    private val disabledStateViewModel = stateViewModelResolver(block.disabled)
    private val highlightedStateViewModel = stateViewModelResolver(block.highlighted)
    private val selectedStateViewModel = stateViewModelResolver(block.selected)

    private val epic = blockViewModel
        .events
        .map { event ->
            when (event) {
                is BlockViewModelInterface.Event.Touched -> ButtonViewModelInterface.Event.DisplayState(
                    highlightedStateViewModel, true, StateOfButton.Highlighted, false
                )
                is BlockViewModelInterface.Event.Released -> ButtonViewModelInterface.Event.DisplayState(
                    normalStateViewModel, true, StateOfButton.Normal, false
                )
                is BlockViewModelInterface.Event.Clicked -> ButtonViewModelInterface.Event.DisplayState(
                    selectedStateViewModel, true, StateOfButton.Selected, true
                )
            }
        }
        .share()

    override val buttonEvents = Observable.concat(
        // start by setting any newly subscribed view to the Normal state!
        Observable.just(
            ButtonViewModelInterface.Event.DisplayState(
                normalStateViewModel, false, StateOfButton.Normal, false
            )
        ),
        // and then subscribe to the event stream as normal
        epic
    )

    override fun viewModelForState(state: StateOfButton): ButtonStateViewModelInterface {
        return when (state) {
            StateOfButton.Selected -> selectedStateViewModel
            StateOfButton.Highlighted -> highlightedStateViewModel
            StateOfButton.Normal -> normalStateViewModel
            StateOfButton.Disabled -> disabledStateViewModel
        }
    }
}
