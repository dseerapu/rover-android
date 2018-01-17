package io.rover.rover.ui.experience.blocks.image

import io.rover.rover.ui.experience.layout.ViewType
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface

class ImageBlockViewModel(
    private val blockViewModel: BlockViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val imageViewModel: ImageViewModelInterface,
    private val borderViewModel: BorderViewModelInterface
) : ImageBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    ImageViewModelInterface by imageViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    BorderViewModelInterface by borderViewModel {
    override val viewType: ViewType = ViewType.Image
}
