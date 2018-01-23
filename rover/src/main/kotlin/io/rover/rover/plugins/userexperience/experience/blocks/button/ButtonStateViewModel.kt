package io.rover.rover.plugins.userexperience.experience.blocks.button

import io.rover.rover.plugins.data.domain.ButtonState
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.TextViewModelInterface

class ButtonStateViewModel(
    val buttonState: ButtonState,
    val borderViewModel: BorderViewModelInterface,
    val backgroundViewModel: BackgroundViewModelInterface,
    val textViewModel: TextViewModelInterface
) : ButtonStateViewModelInterface,
    BorderViewModelInterface by borderViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel
