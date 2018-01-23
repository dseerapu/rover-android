package io.rover.rover.plugins.userexperience.experience.blocks.concerns.border

import io.rover.rover.plugins.data.domain.Border
import io.rover.rover.plugins.userexperience.types.Rect
import io.rover.rover.plugins.userexperience.types.asAndroidColor

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
