package io.rover.rover.ui.viewmodels

import io.rover.rover.core.domain.ButtonBlock
import io.rover.rover.ui.types.RectF


class ButtonViewModel(
    private val block: ButtonBlock
) : ButtonViewModelInterface {
    override val text: String
        // TODO: state support is coming
        get() = block.normal.text
}
