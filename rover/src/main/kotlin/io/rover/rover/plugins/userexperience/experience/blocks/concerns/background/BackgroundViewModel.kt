package io.rover.rover.plugins.userexperience.experience.blocks.concerns.background

import android.graphics.Bitmap
import android.util.DisplayMetrics
import io.rover.rover.plugins.data.domain.Background
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.userexperience.assets.AssetService
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationServiceInterface
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.NetworkTask
import io.rover.rover.plugins.userexperience.types.PixelSize
import io.rover.rover.plugins.userexperience.types.asAndroidColor

class BackgroundViewModel(
    private val background: Background,
    private val assetService: AssetService,
    private val imageOptimizationService: ImageOptimizationServiceInterface
) : BackgroundViewModelInterface {
    override val backgroundColor: Int
        get() = background.backgroundColor.asAndroidColor()

    override fun requestBackgroundImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics,
        callback: (Bitmap, BackgroundImageConfiguration) -> Unit
    ): NetworkTask? {
        val uri = background.backgroundImage?.url
        return if (uri != null) {
            val (urlToFetch, imageConfiguration) =
                imageOptimizationService.optimizeImageBackground(
                    background,
                    targetViewPixelSize,
                    displayMetrics
                ) ?: return null

            assetService.getImageByUrl(urlToFetch.toURL()) { result ->
                val y = when (result) {
                    is NetworkResult.Success -> {
                        callback(
                            result.response,
                            imageConfiguration
                        )
                    }
                    is NetworkResult.Error -> {
                        // TODO perhaps attempt a retry? or should a lower layer attempt retry?
                        // concern should remain here if the experience UI should react or indicate
                        // an error somehow.
                        log.e("Problem retrieving image: ${result.throwable}")
                    }
                }
            }
        } else {
            // log.v("Null URI.  No image set.")
            null
        }
    }
}
