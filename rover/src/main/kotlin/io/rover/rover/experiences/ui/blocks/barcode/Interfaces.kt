package io.rover.rover.experiences.ui.blocks.barcode

import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.*

interface ViewBarcodeInterface {
    var barcodeViewModel: BarcodeViewModelInterface?
}

interface BarcodeViewModelInterface : Measurable, LayoutPaddingDeflection {
    val barcodeType: BarcodeType

    val barcodeValue: String

    enum class BarcodeType {
        PDF417, Code128, Aztec, QrCode
    }
}

interface BarcodeBlockViewModelInterface :
    CompositeBlockViewModelInterface,
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BarcodeViewModelInterface,
    BorderViewModelInterface