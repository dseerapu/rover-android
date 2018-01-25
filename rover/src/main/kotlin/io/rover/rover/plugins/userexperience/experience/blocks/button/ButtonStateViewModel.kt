package io.rover.rover.plugins.userexperience.experience.blocks.button

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.TextViewModelInterface

class ButtonStateViewModel(
    private val borderViewModel: BorderViewModelInterface,
    private val backgroundViewModel: BackgroundViewModelInterface,
    private val textViewModel: TextViewModelInterface
) : ButtonStateViewModelInterface,
    BorderViewModelInterface by borderViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel
