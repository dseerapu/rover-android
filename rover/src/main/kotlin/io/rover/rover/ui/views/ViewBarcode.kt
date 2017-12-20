package io.rover.rover.ui.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.DrawableWrapper
import android.support.v7.widget.AppCompatImageView
import android.widget.ImageView
import io.rover.rover.platform.toAndroidBitmap
import io.rover.rover.ui.types.asAndroidRect
import io.rover.rover.ui.types.dpAsPx
import io.rover.rover.ui.viewmodels.BarcodeViewModelInterface
import io.rover.shaded.zxing.com.google.zxing.BarcodeFormat
import io.rover.shaded.zxing.com.google.zxing.MultiFormatWriter

/**
 * Mixin that binds a barcode view model to an [AppCompatImageView] by rendering the barcodes
 * with ZXing.
 */
class ViewBarcode(
    private val barcodeView: AppCompatImageView
): ViewBarcodeInterface, PaddingContributor {
    init {

        // Using stretch fit because (at least for auto-height) we've ensured that the image will
        // scale aspect-correct, and we also are using integer scaling to ensure a sharp scale of
        // the pixels.  Instead of FIT_CENTER, in the case of Code 128 (the only supported 1D
        // barcode) this allows us to use the GPU to scale the height of the 1px high barcode to fit
        // the view, saving a little bit of memory.

        // However, if it turns out that in the event of non-autoheight blocks we want to ensure
        // aspect correct display of the 2D barcodes (right now the web Experiences App stretches
        // them, and it's not super easy to check to see what iOS SDk 2.0 is currently doing), I
        // will instead need to switch to FIT_CENTER and instead pass in the the dimensions of the
        // view to ZXing below (rather than 0s) to have it do the scaling appropriate to the
        // specific barcode.
        barcodeView.scaleType = ImageView.ScaleType.FIT_XY
    }

    override var barcodeViewModel: BarcodeViewModelInterface? = null
        set(viewModel) {
            field = viewModel
            // TODO: render off-thread (although generation seems fast so it may not matter too
            // much).
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
                        // we want the minimum size, pixel exact.  we'll scale it later.
                        0,
                        0
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

    override val contributedPadding: Rect
        get() = barcodeViewModel?.paddingDeflection?.asAndroidRect() ?: throw RuntimeException("ViewBarcode must be bound to the view model before ViewBlock.") // not a great way to enforce this invariant, alas.
}
