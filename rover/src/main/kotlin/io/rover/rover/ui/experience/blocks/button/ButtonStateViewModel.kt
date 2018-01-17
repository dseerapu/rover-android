package io.rover.rover.ui.experience.blocks.button

import io.rover.rover.core.domain.ButtonState
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonStateViewModelInterface
import io.rover.rover.ui.viewmodels.TextViewModelInterface

class ButtonStateViewModel(
    val buttonState: ButtonState,
    val borderViewModel: BorderViewModelInterface,
    val backgroundViewModel: BackgroundViewModelInterface,
    val textViewModel: TextViewModelInterface
): ButtonStateViewModelInterface,
    BorderViewModelInterface by borderViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel
