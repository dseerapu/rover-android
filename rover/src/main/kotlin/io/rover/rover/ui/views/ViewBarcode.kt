package io.rover.rover.ui.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.DrawableWrapper
import android.support.v7.widget.AppCompatImageView
import android.widget.ImageView
import io.rover.rover.platform.toAndroidBitmap
import io.rover.rover.ui.viewmodels.BarcodeViewModelInterface
import io.rover.shaded.zxing.com.google.zxing.BarcodeFormat
import io.rover.shaded.zxing.com.google.zxing.MultiFormatWriter

/**
 * Mixin that binds a barcode view model to an [AppCompatImageView] by rendering the barcodes
 * with ZXing.
 */
class ViewBarcode(
    private val barcodeView: AppCompatImageView
): ViewBarcodeInterface {
    init {
        barcodeView.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    override var barcodeViewModel: BarcodeViewModelInterface? = null
        set(viewModel) {
            field = viewModel
            // now I have to render the thing.

            // TODO: render off-thread.

            if(viewModel != null) {
                val drawable = BitmapDrawable(
                    barcodeView.resources,
                    MultiFormatWriter().encode(
                        viewModel.barcodeValue,
                        when(viewModel.barcodeType) {
                            BarcodeViewModelInterface.BarcodeType.PDF417 -> BarcodeFormat.PDF_417
                            BarcodeViewModelInterface.BarcodeType.Code128 -> BarcodeFormat.CODE_128
                            BarcodeViewModelInterface.BarcodeType.Aztec -> BarcodeFormat.AZTEC
                            BarcodeViewModelInterface.BarcodeType.QrCode -> BarcodeFormat.QR_CODE
                        },
                        barcodeView.width,
                        barcodeView.height
                    ).toAndroidBitmap()
                ).apply {
                    // The ZXing library appropriately renders the barcodes at their smallest
                    // pixel-exact size.  Thus, we want non-anti-aliased (ie., simple integer
                    // scaling instead of the default bilinear filtering) of the image so as to have
                    // sharp pixel-art for the barcodes, otherwise we'd get a seriously blurry mess.
                    isFilterBitmap = false
                }

                barcodeView.setImageDrawable(
                    drawable
                )
            } else {
                barcodeView.setImageResource(android.R.color.transparent)
            }
        }
}
