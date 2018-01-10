package io.rover.rover.ui.views

import android.graphics.Rect
import io.rover.rover.ui.viewmodels.BackgroundViewModelInterface
import io.rover.rover.ui.viewmodels.BarcodeViewModelInterface
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.viewmodels.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface
import io.rover.rover.ui.viewmodels.ExperienceAppBarViewModelInterface
import io.rover.rover.ui.viewmodels.ExperienceToolbarViewModelInterface
import io.rover.rover.ui.viewmodels.ImageBlockViewModelInterface
import io.rover.rover.ui.viewmodels.LayoutPaddingDeflection
import io.rover.rover.ui.viewmodels.TextBlockViewModelInterface
import io.rover.rover.ui.viewmodels.TextViewModelInterface
import io.rover.rover.ui.viewmodels.WebViewModelInterface

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
 * Binds [BlockViewModelInterface] properties to that of a view.
 *
 * This is responsible for setting padding and anything else relating to block layout.
 */
interface ViewBlockInterface {
    var blockViewModel: BlockViewModelInterface?
}

/**
 * Binds [BackgroundViewModelInterface] properties to that of a view.
 *
 * Backgrounds can specify a background colour or image.
 */
interface ViewBackgroundInterface {
    var backgroundViewModel: BackgroundViewModelInterface?
}

/**
 * Binds [BorderViewModelInterface] properties to that of a view.
 *
 * Borders can specify a border of arbitrary width, with optional rounded corners.
 */
interface ViewBorderInterface {
    var borderViewModel: BorderViewModelInterface?
}

interface ViewTextInterface {
    var textViewModel: TextViewModelInterface?
}

interface ViewBarcodeInterface {
    var barcodeViewModel: BarcodeViewModelInterface?
}

interface ViewWebInterface {
    var webViewModel: WebViewModelInterface?
}

interface ViewImageInterface {
    var imageBlockViewModel: ImageBlockViewModelInterface?
}

interface ViewButtonInterface {
    var buttonViewModel: ButtonViewModelInterface?
}

interface ViewExperienceAppBarInterface {
    var experienceAppBarViewModel: ExperienceAppBarViewModelInterface?
}

interface ViewExperienceToolbarInterface {
    var experienceToolbarViewModel: ExperienceToolbarViewModelInterface?
}