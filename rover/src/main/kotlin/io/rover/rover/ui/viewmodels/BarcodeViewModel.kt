package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.BarcodeBlock
import io.rover.rover.core.domain.BarcodeFormat
import io.rover.rover.ui.MeasurementService
import io.rover.rover.ui.types.Rect
import io.rover.rover.ui.types.RectF

/**
 * Barcode display view model.
 */
class BarcodeViewModel(
    private val barcode: BarcodeBlock,
    private val measurementService: MeasurementService
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
        return measurementService.measureHeightNeededForBarcode(
            barcode.barcodeText,
            barcodeType,
            bounds.width()
        )
    }

    override val paddingDeflection: Rect
        get() = when(barcode.barcodeFormat) {
            BarcodeFormat.Pdf417 -> Rect(5, 5, 5, 5)
            else -> Rect(20, 20, 20, 20)
        }
}
