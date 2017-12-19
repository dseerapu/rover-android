package io.rover.rover.ui.views

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import io.rover.rover.ui.viewmodels.BarcodeBlockViewModelInterface

class BarcodeBlockView : AppCompatImageView, LayoutableView<BarcodeBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val viewBlock = ViewBlock(this)
    private val viewBarcode = ViewBarcode(this)

    override var viewModel: BarcodeBlockViewModelInterface? = null
        set(barcodeBlockViewModel) {
            field = barcodeBlockViewModel

            viewBlock.blockViewModel = barcodeBlockViewModel
            viewBarcode.barcodeViewModel = barcodeBlockViewModel
        }
}
