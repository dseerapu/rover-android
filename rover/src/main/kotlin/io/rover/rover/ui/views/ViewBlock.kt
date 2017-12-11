package io.rover.rover.ui.views

import android.view.View
import io.rover.rover.ui.types.dpAsPx
import io.rover.rover.ui.viewmodels.BlockViewModelInterface

class ViewBlock(
    private val view: View,
    private val paddingContributors: Set<PaddingContributor> = emptySet()
) : ViewBlockInterface {
    // State:
    override var blockViewModel: BlockViewModelInterface? = null
        set(viewModel) {
            field = viewModel
            val displayMetrics = view.resources.displayMetrics

            view.setOnClickListener { viewModel?.click() }

            if (viewModel != null) {
                val contributedPaddings = paddingContributors.map { it.contributedPadding }
                view.setPaddingRelative(
                    (viewModel.insets.left + contributedPaddings.map { it.left }.sum()).dpAsPx(displayMetrics),
                    (viewModel.insets.top + contributedPaddings.map { it.top }.sum()).dpAsPx(displayMetrics),
                    (viewModel.insets.right + contributedPaddings.map { it.right }.sum()).dpAsPx(displayMetrics),
                    (viewModel.insets.bottom + contributedPaddings.map { it.bottom }.sum()).dpAsPx(displayMetrics)
                )
                view.alpha = viewModel.opacity

                view.isClickable = viewModel.isClickable

                // TODO: figure out how to set a ripple drawable for clickable blocks in a way that
                // works across different view types?
            } else {
                view.setPaddingRelative(0, 0, 0, 0)
            }
        }
}
