package io.rover.rover.ui.views

import android.content.Context
import android.util.AttributeSet
import io.rover.rover.ui.viewmodels.RectangleBlockViewModelInterface

class RectangleBlockView: LayoutableView<RectangleBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // mixins
    private val viewBackground = ViewBackground(this)
    private val viewBorder = ViewBorder(this)

    override var viewModel: RectangleBlockViewModelInterface? = null
        set(viewModel) {
            viewBackground.backgroundViewModel = viewModel
            viewBorder.borderViewModel = viewModel
        }
}
