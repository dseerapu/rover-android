package io.rover.rover.ui.experience.blocks.text

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.viewmodels.TextBlockViewModelInterface
import io.rover.rover.ui.views.LayoutableView
import io.rover.rover.ui.views.ViewBackground
import io.rover.rover.ui.views.ViewBlock
import io.rover.rover.ui.experience.blocks.concerns.border.ViewBorder
import io.rover.rover.ui.views.ViewComposition
import io.rover.rover.ui.views.ViewText

class TextBlockView : TextView, LayoutableView<TextBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    // mixins (TODO: injections)
    private val viewComposition = ViewComposition()

    private val viewBackground = ViewBackground(this, viewComposition)
    private val viewBorder = ViewBorder(this, viewComposition)
    private val viewBlock = ViewBlock(this, setOf(viewBorder))
    private val viewText = ViewText(this, AndroidRichTextToSpannedTransformer())

    override var viewModel: TextBlockViewModelInterface? = null
        set(viewModel) {
            viewBorder.borderViewModel = viewModel
            viewBlock.blockViewModel = viewModel
            viewBackground.backgroundViewModel = viewModel
            viewText.textViewModel = viewModel
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

    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
        log.v("Tried to invalidate layout.  Inhibited.")
    }

    override fun forceLayout() {
        log.v("Tried to forcefully invalidate layout.  Inhibited.")
    }
}
