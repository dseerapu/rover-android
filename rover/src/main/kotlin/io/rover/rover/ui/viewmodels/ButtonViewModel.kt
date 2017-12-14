package io.rover.rover.ui.viewmodels

import android.annotation.SuppressLint
import android.os.Parcelable
import io.rover.rover.core.domain.Block
import io.rover.rover.core.domain.BlockAction
import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.core.logging.log
import io.rover.rover.platform.whenNotNull
import io.rover.rover.streams.Observable
import io.rover.rover.streams.PublishSubject
import io.rover.rover.streams.filter
import io.rover.rover.streams.map
import io.rover.rover.streams.share
import io.rover.rover.ui.ViewModelFactoryInterface
import io.rover.rover.ui.types.NavigateTo
import io.rover.rover.ui.types.RectF
import kotlinx.android.parcel.Parcelize

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
        .filter { it is BlockViewModelInterface.Event.Touched || it is BlockViewModelInterface.Event.Released }
        .map { releasedOrTouched ->
            when(releasedOrTouched) {
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
