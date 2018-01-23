@file:JvmName("Interfaces")

package io.rover.rover.plugins.userexperience.experience.blocks.concerns.border

import io.rover.rover.plugins.userexperience.experience.blocks.concerns.layout.LayoutPaddingDeflection

/**
 * Binds [BorderViewModelInterface] properties to that of a view.
 *
 * Borders can specify a border of arbitrary width, with optional rounded corners.
 */
interface ViewBorderInterface {
    var borderViewModel: BorderViewModelInterface?
}

/**
 * This interface is exposed by View Models that have support for a border (of arbitrary width and
 * possibly rounded with a radius).  Equivalent to the [Border] domain model interface.
 */
interface BorderViewModelInterface : LayoutPaddingDeflection {
    val borderColor: Int

    // TODO: this should start returning Px instead of Dp
    val borderRadius: Int

    // TODO: this should start returning Px instead of Dp
    val borderWidth: Int

    companion object
}