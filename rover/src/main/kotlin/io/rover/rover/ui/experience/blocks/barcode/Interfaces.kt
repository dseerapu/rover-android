package io.rover.rover.ui.experience.blocks.barcode

import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutPaddingDeflection
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.ui.experience.blocks.concerns.layout.Measurable

interface ViewBarcodeInterface {
    var barcodeViewModel: BarcodeViewModelInterface?
}

interface BarcodeViewModelInterface: Measurable, LayoutPaddingDeflection {
    val barcodeType: BarcodeType

    val barcodeValue: String

    enum class BarcodeType {
        PDF417, Code128, Aztec, QrCode
    }
}

interface BarcodeBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BarcodeViewModelInterface,
    BorderViewModelInterface