package io.rover.rover.ui.experience.blocks.concerns.layout

import android.graphics.Rect
import io.rover.rover.ui.viewmodels.BlockViewModelInterface

/**
 * Binds [BlockViewModelInterface] properties to that of a view.
 *
 * This is responsible for setting padding and anything else relating to block layout.
 */
interface ViewBlockInterface {
    var blockViewModel: BlockViewModelInterface?
}

/**
 * The View-side equivalent to [LayoutPaddingDeflection].  This View-side parallel structure needs
 * to exist because the View mixins must not set the padding directly on the Android view (lest
 * they clobber one another), and moreover , so they must delegate that responsibility to [ViewBlock] which will
 * gather up all contributed padding and ultimately apply it to the view.
 */
interface PaddingContributor {
    // TODO: consider changing to not use Rect to better indicate that it is not a rectangle but an
    // inset for each edge
    val contributedPadding: Rect
}

/**
 * Exposed by a view model that may need to contribute to the padding around the content.  For
 * instance, the [BorderViewModel] exposes this so that content-bearing view models can ensure their
 * content is not occluded by the border.
 *
 * Note that the View mixins will likely need to implement the [PaddingContributor] interface and
 * ensure that they are passed to the [ViewBlock].  Please see the documentation there for more
 * details and the rationale.
 */
interface LayoutPaddingDeflection {
    // TODO: consider changing to not use Rect to better indicate that it is not a rectangle but an
    // inset for each edge
    val paddingDeflection: io.rover.rover.ui.types.Rect
}