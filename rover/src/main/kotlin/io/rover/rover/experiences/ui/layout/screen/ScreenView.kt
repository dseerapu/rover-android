package io.rover.rover.plugins.userexperience.experience.layout.screen

import android.content.Context
import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import io.rover.rover.plugins.userexperience.experience.layout.BlockAndRowLayoutManager
import io.rover.rover.plugins.userexperience.experience.layout.BlockAndRowRecyclerAdapter
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.ViewComposition
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.ViewBackground
import io.rover.rover.plugins.userexperience.experience.concerns.BindableView

class ScreenView : RecyclerView, BindableView<ScreenViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private val viewComposition = ViewComposition()
    private val viewBackground = ViewBackground(this, viewComposition)

    override var viewModel: ScreenViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            viewBackground.backgroundViewModel = viewModel

            if (viewModel != null) {
                // set up the Experience layout manager for the RecyclerView.  Unlike a typical
                // RecyclerView layout manager, in our system our layout is indeed data, so the
                // layout manager needs the Screen view model.
                layoutManager = BlockAndRowLayoutManager(
                    viewModel,
                    resources.displayMetrics
                )

                // and then setup the adapter itself.
                adapter = BlockAndRowRecyclerAdapter(
                    viewModel.gather()
                )
            }
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
