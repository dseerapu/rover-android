package io.rover.rover.plugins.userexperience.experience.blocks

import io.rover.rover.plugins.data.domain.Block
import io.rover.rover.plugins.data.domain.ButtonState
import io.rover.rover.plugins.data.domain.Row
import io.rover.rover.plugins.data.domain.Screen
import io.rover.rover.plugins.userexperience.experience.blocks.button.ButtonStateViewModelInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.row.RowViewModelInterface
import io.rover.rover.plugins.userexperience.experience.layout.screen.ScreenViewModelInterface

/**
 *
 */
interface BlockViewModelFactoryInterface {
    fun viewModelForBlock(block: Block): BlockViewModelInterface

    fun viewModelForRow(row: Row): RowViewModelInterface

    fun viewModelForScreen(screen: Screen): ScreenViewModelInterface

    fun viewModelForButtonState(buttonState: ButtonState): ButtonStateViewModelInterface
}