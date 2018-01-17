package io.rover.rover.ui.experience.blocks.rectangle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import io.rover.rover.core.logging.log
import io.rover.rover.ui.viewmodels.RectangleBlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutableView
import io.rover.rover.ui.experience.blocks.concerns.background.ViewBackground
import io.rover.rover.ui.experience.blocks.concerns.border.ViewBorder
import io.rover.rover.ui.experience.blocks.concerns.ViewComposition

class RectangleBlockView : View, LayoutableView<RectangleBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // mixins
    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this, viewComposition)
    private val viewBorder = ViewBorder(this, viewComposition)

    override var viewModel: RectangleBlockViewModelInterface? = null
        set(viewModel) {
            viewBackground.backgroundViewModel = viewModel
            viewBorder.borderViewModel = viewModel
        }

    override val view: View
        get() = this

    override fun onDraw(canvas: Canvas) {
        viewComposition.beforeOnDraw(canvas)
        super.onDraw(canvas)
        viewComposition.afterOnDraw(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComposition.onSizeChanged(w, h, oldw, oldh)
    }

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }
}
