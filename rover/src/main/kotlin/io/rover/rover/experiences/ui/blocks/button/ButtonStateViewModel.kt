package io.rover.rover.experiences.ui.blocks.button

import io.rover.rover.experiences.ui.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.text.TextViewModelInterface

class ButtonStateViewModel(
    private val borderViewModel: BorderViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val textViewModel: TextViewModelInterface
) : ButtonStateViewModelInterface,
    BorderViewModelInterface by borderViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel
