package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.ButtonState

class ButtonStateViewModel(
    val buttonState: ButtonState,
    val borderViewModel: BorderViewModelInterface,
    val backgroundViewModel: BackgroundViewModelInterface,
    val textViewModel: TextViewModelInterface
): ButtonStateViewModelInterface,
    BorderViewModelInterface by borderViewModel,
    BackgroundViewModelInterface by backgroundViewModel,
    TextViewModelInterface by textViewModel
