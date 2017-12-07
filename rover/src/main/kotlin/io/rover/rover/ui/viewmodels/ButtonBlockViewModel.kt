package io.rover.rover.ui.viewmodels

import io.rover.rover.ui.types.ViewType

class ButtonBlockViewModel(
    blockViewModel: BlockViewModelInterface,
    buttonViewModel: ButtonViewModelInterface
): ButtonBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    ButtonViewModelInterface by buttonViewModel
{
    override val viewType: ViewType = ViewType.Button
}
