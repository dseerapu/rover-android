package io.rover.rover.experiences.ui.blocks.concerns.border

import io.rover.rover.core.data.domain.Border
import io.rover.rover.experiences.types.Rect
import io.rover.rover.experiences.types.asAndroidColor

class BorderViewModel(
    val border: Border
) : BorderViewModelInterface {
    override val borderColor: Int
        get() = border.borderColor.asAndroidColor()

    override val borderRadius: Int
        get() = border.borderRadius

    override val borderWidth: Int
        get() = border.borderWidth

    override val paddingDeflection: Rect
        get() = Rect(
            border.borderWidth,
            border.borderWidth,
            border.borderWidth,
            border.borderWidth
        )
}
