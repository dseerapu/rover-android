package io.rover.rover.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import io.rover.rover.core.logging.log
import io.rover.rover.streams.subscribe
import io.rover.rover.ui.AndroidRichTextToSpannedTransformer
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface
import io.rover.rover.ui.viewmodels.StateOfButton

class ButtonBlockView : FrameLayout, LayoutableView<ButtonBlockViewModelInterface> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val disabledView: ButtonStateView = ButtonStateView(context)
    private val highlightedView: ButtonStateView = ButtonStateView(context)
    private val normalView: ButtonStateView = ButtonStateView(context)
    private val selectedView: ButtonStateView = ButtonStateView(context)

    private val textView: TextView = TextView(context)

    private val viewText: ViewText = ViewText(textView, AndroidRichTextToSpannedTransformer())

    private val viewBlock = ViewBlock(this, setOf())

    init {
        addView(disabledView)
        addView(highlightedView)
        addView(normalView)
        addView(selectedView)
        addView(textView)

        disabledView.visibility = View.INVISIBLE
        highlightedView.visibility = View.INVISIBLE
        normalView.visibility = View.INVISIBLE
        selectedView.visibility = View.INVISIBLE

    }

    override var viewModel: ButtonBlockViewModelInterface? = null
        set(buttonBlockViewModel) {
            field = buttonBlockViewModel

            viewBlock.blockViewModel = buttonBlockViewModel

            disabledView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Disabled)
            normalView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Normal)
            highlightedView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Highlighted)
            selectedView.viewModel = buttonBlockViewModel?.viewModelForState(StateOfButton.Selected)

            buttonBlockViewModel?.buttonEvents?.subscribe({ event ->
                when(event) {
                    is ButtonViewModelInterface.Event.DisplayState -> {
                        viewText.textViewModelInterface = event.viewModel

                        val viewStateBeingTransitionedTo = event.stateOfButton

                        disabledView.visibility = if (viewStateBeingTransitionedTo == StateOfButton.Disabled) View.VISIBLE else View.INVISIBLE
                        normalView.visibility = if (viewStateBeingTransitionedTo == StateOfButton.Normal) View.VISIBLE else View.INVISIBLE
                        highlightedView.visibility = if (viewStateBeingTransitionedTo == StateOfButton.Highlighted) View.VISIBLE else View.INVISIBLE
                        selectedView.visibility = if (viewStateBeingTransitionedTo == StateOfButton.Selected) View.VISIBLE else View.INVISIBLE
                    }
                }
            },
                { error -> throw(RuntimeException("Button block view subscription to view model error", error)) }
            )
        }
}
