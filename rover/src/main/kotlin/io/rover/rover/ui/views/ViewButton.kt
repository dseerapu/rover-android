package io.rover.rover.ui.views

import android.support.v7.widget.AppCompatButton
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface

class ViewButton(
    // TODO: we are likely to stop using Android button because it apparently does not support
    // all of our requirements.
    private val buttonView: AppCompatButton
) : ViewButtonInterface {
    override var buttonViewModel: ButtonViewModelInterface? = null
        set(viewModel) {
            field = viewModel
            buttonView.text = viewModel?.text ?: ""
        }
}
