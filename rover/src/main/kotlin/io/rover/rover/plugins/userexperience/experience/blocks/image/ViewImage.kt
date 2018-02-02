package io.rover.rover.plugins.userexperience.experience.blocks.image

import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.widget.ImageView
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.androidLifecycleDispose
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.whenNotNull
import io.rover.rover.plugins.data.http.NetworkTask
import io.rover.rover.plugins.userexperience.experience.ViewModelBinding
import io.rover.rover.plugins.userexperience.types.PixelSize

/**
 * Mixin that binds an image block view model to the relevant parts of an [ImageView].
 */
class ViewImage(
    private val imageView: AppCompatImageView
) : ViewImageInterface {
    private val shortAnimationDuration = imageView.resources.getInteger(
        android.R.integer.config_shortAnimTime
    )

    // State:
    private val dimensionCallbacks: MutableSet<DimensionCallback> = mutableSetOf()
    private var width: Int = 0
    private var height: Int = 0

    init {
        imageView.scaleType = ImageView.ScaleType.FIT_XY

        // in order to know our realized width and height we need to listen for layout events.

        // TODO: setting these initial values here might be the cause of the issues with images
        // getting the wrong density loaded when you fling.
        width = imageView.width
        height = imageView.height
        imageView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            width = right - left
            height = bottom - top
            dimensionCallbacks.forEach { it.invoke(width, height) }
            dimensionCallbacks.clear()
        }
    }

    /**
     * If dimensions are ready (or when they become ready), execute the given callback.
     */
    private fun whenDimensionsReady(callback: DimensionCallback) {
        if (width == 0 && height == 0) {
            // dimensions aren't ready. wait.
            // log.v("Dimensions aren't ready.  Waiting.")
            dimensionCallbacks.add(callback)
        } else {
            // log.v("Dimensions are already ready.  Firing immediately.")
            callback(width, height)
        }
    }

    override var imageBlockViewModel: ImageBlockViewModelInterface? by ViewModelBinding() { viewModel, subscriptionCallback ->
            if (viewModel != null) {
                // and also clear any waiting layout callbacks
                dimensionCallbacks.clear()

                imageView.alpha = 0f

                // we need to know the laid out dimensions of the view in order to ask the view
                // model for an optimized version of the image suited to the view's size.
                whenDimensionsReady { width, height ->
                    viewModel.requestImage(
                        PixelSize(width, height),
                        imageView.resources.displayMetrics
                    ).androidLifecycleDispose(imageView)
                        .subscribe({ bitmap ->
                        imageView.setImageBitmap(bitmap)
                        imageView.animate()
                            .alpha(viewModel.opacity)
                            .setDuration(shortAnimationDuration.toLong())
                            .start()
                    }, { error -> throw(error) }, { subscriptionCallback(it) }
                    )
                }
            }
        }
}
