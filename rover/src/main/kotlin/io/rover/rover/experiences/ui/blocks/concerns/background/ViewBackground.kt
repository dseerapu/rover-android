package io.rover.rover.plugins.userexperience.experience.blocks.concerns.background

import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.view.Gravity
import android.view.View
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.DrawableWrapper
import io.rover.rover.experiences.ui.ViewModelBinding
import io.rover.rover.plugins.userexperience.experience.blocks.image.DimensionCallback
import io.rover.rover.experiences.types.PixelSize
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.ViewCompositionInterface

class ViewBackground(
    private val view: View,
    viewComposition: ViewCompositionInterface
) : ViewBackgroundInterface {
    private val shortAnimationDuration = view.resources.getInteger(
        android.R.integer.config_shortAnimTime
    )

    // State:
    private val dimensionCallbacks: MutableSet<DimensionCallback> = mutableSetOf()
    private var width: Int = 0
    private var height: Int = 0

    init {
        // in order to know our realized width and height we need to listen for layout events.
        viewComposition.registerOnSizeChangedCallback { width, height, _, _ ->
            dimensionCallbacks.forEach { it.invoke(width, height) }
            dimensionCallbacks.clear()
        }
    }

    /**
     * If dimensions are ready (or when they become ready), execute the given callback.
     */
    private fun whenDimensionsReady(callback: DimensionCallback) {
        if (width == 0 && height == 0) {
            // log.v("Dimensions aren't ready.  Waiting.")
            dimensionCallbacks.add(callback)
        } else {
            // log.v("Dimensions are already ready.  Firing immediately.")
            callback(width, height)
        }
    }

    override var backgroundViewModel: BackgroundViewModelInterface? by ViewModelBinding { viewModel, subscriptionCallback ->
        view.background = null
        view.setBackgroundColor(Color.TRANSPARENT)

        if (viewModel != null) {
            view.background = null
            view.setBackgroundColor(viewModel.backgroundColor)

            // TODO: both dimensions coming ready and the background image becoming ready are
            // async. we want to wait for both, and also *unsubscribe* them when this view is bound to a new view model.
            whenDimensionsReady { width, height ->
                viewModel.requestBackgroundImage(
                    PixelSize(width, height),
                    view.resources.displayMetrics
                ).androidLifecycleDispose(this.view)
                    .subscribe({ (bitmap, backgroundImageConfiguration) ->
                        // now construct/compose drawables for the given configuration and bitmap.

                        // note that this will only have an effect in tiled mode (which is exactly
                        // where we need it), since we always scale to the insets otherwise.
                        bitmap.density = backgroundImageConfiguration.imageNativeDensity

                        val bitmapDrawable =
                            BitmapDrawable(
                                view.resources,
                                bitmap
                            ).apply {
                                this.gravity = Gravity.FILL
                                if (backgroundImageConfiguration.tileMode != null) {
                                    tileModeX = backgroundImageConfiguration.tileMode
                                    tileModeY = backgroundImageConfiguration.tileMode
                                }
                            }

                        val backgroundDrawable = BackgroundColorDrawableWrapper(
                            viewModel.backgroundColor,
                            InsetDrawable(
                                bitmapDrawable,
                                backgroundImageConfiguration.insets.left,
                                backgroundImageConfiguration.insets.top,
                                backgroundImageConfiguration.insets.right,
                                backgroundImageConfiguration.insets.bottom
                            )
                        )

                        ObjectAnimator.ofInt(
                            backgroundDrawable, "alpha", 0, 255
                        ).apply {
                            duration = shortAnimationDuration.toLong()
                            start()
                        }

                        view.background = backgroundDrawable
                    }, { error -> throw(error) }, { subscription -> subscriptionCallback(subscription) })

            }
        }
    }
}

class BackgroundColorDrawableWrapper(
    private val backgroundColor: Int,
    private val drawableOnTopOfColor: Drawable
) : DrawableWrapper(drawableOnTopOfColor) {

    override fun draw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        drawableOnTopOfColor.draw(canvas)
    }
}
