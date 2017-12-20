package io.rover.rover.ui.views

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import io.rover.rover.ui.viewmodels.BarcodeBlockViewModelInterface

class BarcodeBlockView : AppCompatImageView, LayoutableView<BarcodeBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this)
    private val viewBorder = ViewBorder(this, viewComposition)
    private val viewBarcode = ViewBarcode(this)
    private val viewBlock = ViewBlock(this, setOf(viewBorder, viewBarcode))

    override var viewModel: BarcodeBlockViewModelInterface? = null
        set(barcodeBlockViewModel) {
            field = barcodeBlockViewModel

            viewBorder.borderViewModel = barcodeBlockViewModel
            viewBarcode.barcodeViewModel = barcodeBlockViewModel
            viewBlock.blockViewModel = barcodeBlockViewModel
            viewBackground.backgroundViewModel = barcodeBlockViewModel
        }
}
