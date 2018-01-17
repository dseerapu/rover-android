package io.rover.rover.ui.experience.blocks.image

import io.rover.rover.ui.types.ViewType
import io.rover.rover.ui.viewmodels.BackgroundViewModelInterface
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.viewmodels.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.ImageBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ImageViewModelInterface

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
