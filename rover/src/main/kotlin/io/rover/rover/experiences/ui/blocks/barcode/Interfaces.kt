package io.rover.rover.plugins.userexperience.experience.blocks.barcode

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.*

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