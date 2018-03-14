package io.rover.rover.experiences.ui.blocks.button

import io.rover.rover.experiences.ui.layout.ViewType
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface

class ButtonBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    buttonViewModel: ButtonViewModelInterface
) : ButtonBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    ButtonViewModelInterface by buttonViewModel {
    override val viewType: ViewType = ViewType.Button
}
