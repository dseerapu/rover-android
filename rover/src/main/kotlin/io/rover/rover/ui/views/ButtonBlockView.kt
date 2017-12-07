package io.rover.rover.ui.views

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.widget.Button
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface

class ButtonBlockView : AppCompatButton, LayoutableView<ButtonBlockViewModelInterface> {
    // TODO: this will probably change to not use a stock Android button.
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val viewButton = ViewButton(this)

    override var viewModel: ButtonBlockViewModelInterface? = null
        set(value) {
            viewButton.buttonViewModel = value
        }
}
