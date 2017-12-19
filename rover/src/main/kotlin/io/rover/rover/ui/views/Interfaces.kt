package io.rover.rover.ui.views

import android.graphics.Rect
import io.rover.rover.ui.viewmodels.BackgroundViewModelInterface
import io.rover.rover.ui.viewmodels.BarcodeViewModelInterface
import io.rover.rover.ui.viewmodels.BlockViewModelInterface
import io.rover.rover.ui.viewmodels.BorderViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonBlockViewModelInterface
import io.rover.rover.ui.viewmodels.ButtonViewModelInterface
import io.rover.rover.ui.viewmodels.ImageBlockViewModelInterface
import io.rover.rover.ui.viewmodels.TextBlockViewModelInterface
import io.rover.rover.ui.viewmodels.TextViewModelInterface
import io.rover.rover.ui.viewmodels.WebViewModelInterface

interface PaddingContributor {
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
