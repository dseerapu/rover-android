package io.rover.rover.ui.views

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import io.rover.rover.ui.BlockAndRowLayoutManager
import io.rover.rover.ui.BlockAndRowRecyclerAdapter
import io.rover.rover.ui.viewmodels.ScreenViewModelInterface

class ScreenView : RecyclerView, BindableView<ScreenViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override var viewModel: ScreenViewModelInterface? = null
        set(viewModel) {
            field = viewModel

            if(viewModel != null) {
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
}
