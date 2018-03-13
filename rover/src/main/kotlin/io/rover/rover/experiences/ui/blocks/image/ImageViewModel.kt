package io.rover.rover.plugins.userexperience.experience.blocks.image

import android.graphics.Bitmap
import android.util.DisplayMetrics
import io.rover.rover.core.data.domain.ImageBlock
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.flatMap
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.experiences.types.PixelSize
import io.rover.rover.experiences.types.RectF
import java.net.URL

class ImageViewModel(
    private val block: ImageBlock,
    private val assetService: AssetService,
    private val imageOptimizationService: ImageOptimizationServiceInterface
) : ImageViewModelInterface {

    override fun requestImage(
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics
    ): Publisher<Bitmap> {
        val uri = block.image?.url

        return if (uri != null) {
            log.v("There is an image to retrieve.  Starting.")

            val uriWithParameters = imageOptimizationService.optimizeImageBlock(
                block,
                targetViewPixelSize,
                displayMetrics
            )

            val url = URL(uriWithParameters.toString())

            assetService.getImageByUrl(url).flatMap { result ->
                when (result) {
                    is NetworkResult.Success -> Observable.just(result.response)
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

    override fun intrinsicHeight(bounds: RectF): Float {
        val image = block.image

        return if (image == null) {
            // no image set means no height at all.
            0f
        } else {
            // get aspect ratio of image and use it to calculate the height needed to accommodate
            // the image at its correct aspect ratio given the width
            val heightToWidthRatio = image.height.toFloat() / image.width.toFloat()
            bounds.width() * heightToWidthRatio
        }
    }
}
