@file:JvmName("Interfaces")

package io.rover.rover.ui.experience.blocks.concerns.background

import android.graphics.Bitmap
import android.graphics.Shader
import android.util.DisplayMetrics
import io.rover.rover.services.network.NetworkTask
import io.rover.rover.ui.types.Rect
import io.rover.rover.ui.types.PixelSize

/**
 * Binds [BackgroundViewModelInterface] properties to that of a view.
 *
 * Backgrounds can specify a background colour or image.
 */
interface ViewBackgroundInterface {
    var backgroundViewModel: BackgroundViewModelInterface?
}

/**
 * Specifies how the view should properly display the given background image.
 *
 * The method these are specified with is a bit idiosyncratic on account of Android implementation
 * details and the combination of Drawables the view uses to achieve the effect.
 */
class BackgroundImageConfiguration(
    /**
     * Bounds in pixels, in *relative insets from their respective edges*.
     *
     * TODO: consider changing to not use Rect to better indicate that it is not a rectangle but an inset for each edge
     *
     * Our drawable is always set to FILL_XY, which means by specifying these insets you get
     * complete control over the aspect ratio, sizing, and positioning.  Note that this parameter
     * cannot be used to specify any sort of scaling for tiling, since the bottom/right bounds are
     * effectively undefined as the pattern repeats forever.  In that case, consider using using
     * [imageNativeDensity] to achieve a scale effect (although note that it is in terms of the
     * display DPI).
     *
     * (Note: we use this approach rather than just having a configurable gravity on the drawable
     * because that would not allow for aspect correct fit scaling.)
     */
    val insets: Rect,

    /**
     * An Android tiling mode.  For no tiling, set as null.
     */
    val tileMode: Shader.TileMode?,

    /**
     * This density value should be set on the bitmap with [Bitmap.setDensity] before drawing it
     * on an Android canvas.
     */
    val imageNativeDensity: Int
)

/**
 * This interface is exposed by View Models that have support for a background.  Equivalent to
 * the [Background] domain model interface.
 */
interface BackgroundViewModelInterface {
    val backgroundColor: Int

    fun requestBackgroundImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics,
        callback: (
            /**
             * The bitmap to be drawn.  It is recommended that the consumer arrange to have it
             * scaled to a roughly appropriate amount (need not be exact; that is the purpose of the
             * view size and the [insets] given above) and also to be uploaded to GPU texture memory
             * off thread ([Bitmap.prepareToDraw]) before setting it.
             *
             * Note: one can set the source density of the bitmap to control its scaling (which is
             * particularly relevant for tile modes where
             */
            Bitmap,

            BackgroundImageConfiguration
        ) -> Unit
    ): NetworkTask?
}