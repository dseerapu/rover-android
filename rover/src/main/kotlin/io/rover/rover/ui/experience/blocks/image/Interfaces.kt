@file:JvmName("Interfaces")

package io.rover.rover.ui.experience.blocks.image

import android.graphics.Bitmap
import android.util.DisplayMetrics
import io.rover.rover.services.network.NetworkTask
import io.rover.rover.ui.experience.blocks.concerns.background.BackgroundViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.border.BorderViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.BlockViewModelInterface
import io.rover.rover.ui.experience.blocks.concerns.layout.LayoutableViewModel
import io.rover.rover.ui.types.PixelSize
import io.rover.rover.ui.experience.blocks.concerns.layout.Measurable

interface ViewImageInterface {
    var imageBlockViewModel: ImageBlockViewModelInterface?
}

interface ImageViewModelInterface : Measurable {
    // TODO: I may elect to demote the Bitmap concern from the ViewModel into just the View (or a
    // helper of some kind) in order to avoid a thick Android object (Bitmap) being touched here

    /**
     * Get the needed image for display, hitting caches if possible and the network if necessary.
     * You'll need to give a [PixelSize] of the target view the image will be landing in.  This will
     * allow for optimizations to select, download, and cache the appropriate size of content.
     *
     * Remember to call [NetworkTask.resume] to start the retrieval, or your callback will never
     * be hit.
     */
    fun requestImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics,
        callback: (Bitmap) -> Unit
    ): NetworkTask?
}

interface ImageBlockViewModelInterface :
    LayoutableViewModel,
    BlockViewModelInterface,
    BackgroundViewModelInterface,
    BorderViewModelInterface,
    ImageViewModelInterface

typealias DimensionCallback = (width: Int, height: Int) -> Unit
