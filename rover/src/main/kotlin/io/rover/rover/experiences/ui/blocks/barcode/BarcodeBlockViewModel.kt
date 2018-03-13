package io.rover.rover.plugins.userexperience.experience.blocks.barcode

import io.rover.rover.experiences.types.Rect
import io.rover.rover.plugins.userexperience.experience.layout.ViewType
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface

class BarcodeBlockViewModel(
    private val blockViewModel: BlockViewModelInterface,
    private val barcodeViewModel: BarcodeViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val borderViewModel: BorderViewModelInterface
) : BarcodeBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    BorderViewModelInterface by borderViewModel,
    BarcodeViewModelInterface by barcodeViewModel {
    override val viewType: ViewType = ViewType.Barcode

    override val paddingDeflection: Rect
        // Both the border and the barcode itself are contributing insets/padding, so add them
        // together.
        get() = Rect(
            borderViewModel.paddingDeflection.left + barcodeViewModel.paddingDeflection.left,
            borderViewModel.paddingDeflection.top + barcodeViewModel.paddingDeflection.top,
            borderViewModel.paddingDeflection.right + barcodeViewModel.paddingDeflection.right,
            borderViewModel.paddingDeflection.bottom + barcodeViewModel.paddingDeflection.bottom
        )
}
