package io.rover.rover.plugins.userexperience.experience.blocks.barcode

import io.rover.rover.plugins.data.domain.BarcodeBlock
import io.rover.rover.plugins.data.domain.BarcodeFormat
import io.rover.rover.plugins.userexperience.MeasurementService
import io.rover.rover.plugins.userexperience.types.Rect
import io.rover.rover.plugins.userexperience.types.RectF

/**
 * Barcode display view model.
 */
class BarcodeViewModel(
    private val barcode: BarcodeBlock,
    private val measurementService: MeasurementService
) : BarcodeViewModelInterface {
    override val barcodeType: BarcodeViewModelInterface.BarcodeType
        get() = when (barcode.barcodeFormat) {
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
        get() = when (barcode.barcodeFormat) {
            BarcodeFormat.Pdf417 -> Rect(5, 5, 5, 5)
            else -> Rect(20, 20, 20, 20)
        }
}
