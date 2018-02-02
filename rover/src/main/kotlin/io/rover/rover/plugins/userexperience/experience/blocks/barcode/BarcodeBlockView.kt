package io.rover.rover.plugins.userexperience.experience.blocks.barcode

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutableView
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.ViewBackground
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.ViewBlock
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.ViewBorder
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.ViewComposition

class BarcodeBlockView : AppCompatImageView, LayoutableView<BarcodeBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this, viewComposition)
    private val viewBorder = ViewBorder(this, viewComposition)
    private val viewBarcode = ViewBarcode(this)
    private val viewBlock = ViewBlock(this, setOf(viewBorder, viewBarcode))

    override var viewModel: BarcodeBlockViewModelInterface? by ViewModelBinding { viewModel, _ ->
        viewBorder.borderViewModel = viewModel
        viewBarcode.barcodeViewModel = viewModel
        viewBlock.blockViewModel = viewModel
        viewBackground.backgroundViewModel = viewModel
    }

    override fun onDraw(canvas: Canvas) {
        viewComposition.beforeOnDraw(canvas)
        super.onDraw(canvas)
        viewComposition.afterOnDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComposition.onSizeChanged(w, h, oldw, oldh)
    }
}
