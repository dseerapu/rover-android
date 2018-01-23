package io.rover.rover.plugins.userexperience.experience.blocks.button

import io.rover.rover.plugins.userexperience.experience.layout.ViewType
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface

class ButtonBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    buttonViewModel: ButtonViewModelInterface
) : ButtonBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    ButtonViewModelInterface by buttonViewModel {
    override val viewType: ViewType = ViewType.Button
}
