package io.rover.rover.experiences.ui.layout.row

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.View
import io.rover.rover.experiences.ui.blocks.concerns.ViewComposition
import io.rover.rover.experiences.ui.blocks.concerns.background.ViewBackground
import io.rover.rover.experiences.ui.blocks.concerns.layout.LayoutableView

class RowView : View, LayoutableView<RowViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this, viewComposition)

    override var viewModel: RowViewModelInterface? = null
        set(viewModel) {
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
