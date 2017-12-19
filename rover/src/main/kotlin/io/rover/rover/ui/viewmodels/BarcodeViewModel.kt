package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.BarcodeBlock
import io.rover.rover.core.domain.BarcodeFormat
import io.rover.rover.ui.types.RectF

/**
 * Barcode display view model.
 */
class BarcodeViewModel(
    private val barcode: BarcodeBlock
    // TODO: may need MeasurementService (and to teach it to talk to ZXing)
): BarcodeViewModelInterface {
    override val barcodeType: BarcodeViewModelInterface.BarcodeType
        get() = when(barcode.barcodeFormat) {
            BarcodeFormat.AztecCode -> BarcodeViewModelInterface.BarcodeType.Aztec
            BarcodeFormat.Code128 -> BarcodeViewModelInterface.BarcodeType.Code128
            BarcodeFormat.Pdf417 -> BarcodeViewModelInterface.BarcodeType.PDF417
            BarcodeFormat.QrCode -> BarcodeViewModelInterface.BarcodeType.QrCode
        }

    override val barcodeValue: String
        get() = barcode.barcodeText

    override fun intrinsicHeight(bounds: RectF): Float {
        // TODO: figure out measurement rules for the various types of barcode
        return 40f
    }
}
