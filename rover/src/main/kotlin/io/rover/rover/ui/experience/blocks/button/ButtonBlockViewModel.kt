package io.rover.rover.ui.experience.blocks.button

import io.rover.rover.ui.types.ViewType
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface

class ButtonBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    buttonViewModel: ButtonViewModelInterface
): ButtonBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    ButtonViewModelInterface by buttonViewModel
{
    override val viewType: ViewType = ViewType.Button
}
