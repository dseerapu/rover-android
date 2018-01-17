package io.rover.rover.ui.experience.blocks.text

import io.rover.rover.ui.types.ViewType
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.TextBlockViewModelInterface
import io.rover.rover.ui.viewmodels.TextViewModelInterface

class TextBlockViewModel(
    private val blockViewModel: BlockViewModelInterface,
    private val textViewModel: TextViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val borderViewModel: BorderViewModelInterface
) : TextBlockViewModelInterface,
    BlockViewModelInterface by blockViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel,
    BorderViewModelInterface by borderViewModel {

    override val viewType: ViewType = ViewType.Text
}
