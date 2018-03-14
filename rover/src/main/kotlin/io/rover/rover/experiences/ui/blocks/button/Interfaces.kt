package io.rover.rover.experiences.ui.blocks.button

import io.rover.rover.core.streams.Observable
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModel
import io.rover.rover.experiences.ui.blocks.concerns.layout.CompositeBlockViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.text.TextViewModelInterface
import io.rover.rover.experiences.ui.concerns.BindableViewModel

/**
 * View Model for block content that contains a clickable button with several different
 * states.
 *
 * Note that if you're looking for the Click event, that is handled more generally in
 * [BlockViewModel].
 */
interface ButtonViewModelInterface {
    val buttonEvents: Observable<Event>

    sealed class Event {
        /**
         * Reveal the text for the given state.
         */
        data class DisplayState(
            val viewModel: ButtonStateViewModelInterface,
            val animate: Boolean,

            /**
             * The owning view will maintain a set of background views itself for allowing for
             * partially occlusive transitions between button states.  This means it has need to
             * know which of the backgrounds it should display on a given [Event.DisplayState]
             * event.
             */
            val stateOfButton: StateOfButton,

            /**
             * The given animation should undo itself afterwards.
             */
            val selfRevert: Boolean
        ) : Event()
    }

    /**
     * The owning view will maintain a set of background views itself for allowing for partially
     * occlusive transitions between button states.  This means it has need to interrogate to
     * bind all of the needed view models to each of the background layers.
     */
    fun viewModelForState(state: StateOfButton): ButtonStateViewModelInterface
}

interface ButtonBlockViewModelInterface :
    CompositeBlockViewModelInterface,
    LayoutableViewModel,
    BlockViewModelInterface,
    ButtonViewModelInterface

interface ButtonStateViewModelInterface :
    BindableViewModel,
    TextViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface

enum class StateOfButton {
    Normal,
    Disabled,
    Highlighted,
    Selected
}