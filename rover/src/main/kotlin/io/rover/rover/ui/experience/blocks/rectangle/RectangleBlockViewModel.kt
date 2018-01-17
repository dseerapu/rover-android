package io.rover.rover.ui.experience.blocks.rectangle

import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModel
import io.rover.rover.ui.types.ViewType
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.RectangleBlockViewModelInterface

class RectangleBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    backgroundViewModel: BackgroundViewModelInterface,
    borderViewModel: BorderViewModel
) : RectangleBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    BorderViewModelInterface by borderViewModel {
    override val viewType: ViewType = ViewType.Rectangle
}
