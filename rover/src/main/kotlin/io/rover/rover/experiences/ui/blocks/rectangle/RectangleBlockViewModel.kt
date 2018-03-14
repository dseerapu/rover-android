package io.rover.rover.plugins.userexperience.experience.blocks.rectangle

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModel
import io.rover.rover.plugins.userexperience.experience.layout.ViewType
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface

class RectangleBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    backgroundViewModel: BackgroundViewModelInterface,
    borderViewModel: BorderViewModelInterface
) : RectangleBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    BorderViewModelInterface by borderViewModel {
    override val viewType: ViewType = ViewType.Rectangle
}
