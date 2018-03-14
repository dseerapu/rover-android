package io.rover.rover.experiences.ui.blocks

import io.rover.rover.core.data.domain.Block
import io.rover.rover.core.data.domain.ButtonState
import io.rover.rover.core.data.domain.Row
import io.rover.rover.core.data.domain.Screen
import io.rover.rover.experiences.ui.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.experiences.ui.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.experiences.ui.layout.row.RowViewModelInterface
import io.rover.rover.experiences.ui.layout.screen.ScreenViewModelInterface

/**
 *
 */
interface BlockViewModelFactoryInterface {
    fun viewModelForBlock(block: Block): BlockViewModelInterface

    fun viewModelForRow(row: Row): RowViewModelInterface

    fun viewModelForScreen(screen: Screen): ScreenViewModelInterface

    fun viewModelForButtonState(buttonState: ButtonState): ButtonStateViewModelInterface
}