package io.rover.rover.plugins.userexperience.experience.blocks.concerns.background

import android.graphics.Bitmap
import android.util.DisplayMetrics
import io.rover.rover.core.data.domain.Background
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.experiences.types.PixelSize
import io.rover.rover.experiences.types.asAndroidColor

class BackgroundViewModel(
    private val background: Background,
    private val assetService: AssetService,
    private val imageOptimizationService: ImageOptimizationServiceInterface
) : BackgroundViewModelInterface {
    override val backgroundColor: Int
        get() = background.backgroundColor.asAndroidColor()

    override fun requestBackgroundImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics
    ): Publisher<Pair<Bitmap, BackgroundImageConfiguration>> {
        val uri = background.backgroundImage?.url
        return if (uri != null) {
            val (urlToFetch, imageConfiguration) =
                imageOptimizationService.optimizeImageBackground(
                    background,
                    targetViewPixelSize,
                    displayMetrics
                ) ?: return Observable.empty()

            assetService.getImageByUrl(urlToFetch.toURL()).flatMap { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Observable.just(
                            Pair(
                                result.response,
                                imageConfiguration
                            )
                        )
                    }
                    is NetworkResult.Error -> {
                        // TODO perhaps attempt a retry? or should a lower layer attempt retry?
                        // concern should remain here if the experience UI should react or indicate
                        // an error somehow.
                        log.e("Problem retrieving image: ${result.throwable}")
                        Observable.empty()
                    }
                }
            }
        } else {
            // log.v("Null URI.  No image set.")
           Observable.empty()
        }
    }
}
