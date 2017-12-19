package io.rover.rover.ui.viewmodels

import io.rover.rover.ui.types.ViewType

class BarcodeBlockViewModel(
    private val blockViewModel: BlockViewModelInterface,
    private val barcodeViewModel: BarcodeViewModelInterface
) : BarcodeBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BarcodeViewModelInterface by barcodeViewModel {
    override val viewType: ViewType = ViewType.Barcode
}
