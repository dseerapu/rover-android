package io.rover.rover.plugins.userexperience.assets

import android.graphics.Bitmap
import android.util.DisplayMetrics
import io.rover.rover.core.streams.Publisher
import io.rover.rover.plugins.data.domain.Background
import io.rover.rover.plugins.data.domain.ImageBlock
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.userexperience.types.PixelSize
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.background.BackgroundImageConfiguration
import java.net.URI
import java.net.URL

/**
 * A pipeline step that does I/O operations of (fetch, cache, etc.) an asset.
 *
 * Stages will synchronously block the thread while waiting for I/O.
 *
 * Stages should not block to do computation-type work, however; the asset pipeline is
 * run on a thread pool optimized for I/O multiplexing and not computation.
 */
interface SynchronousPipelineStage<in TInput, TOutput> {
    fun request(input: TInput): PipelineStageResult<TOutput>
}

sealed class PipelineStageResult<TOutput> {
    class Successful<TOutput>(val output: TOutput) : PipelineStageResult<TOutput>()
    class Failed<TOutput>(val reason: Throwable) : PipelineStageResult<TOutput>()
}

interface AssetService {
    /**
     * Retrieve the needed photo, from caches if possible.
     *
     * [completionHandler] will be called on app's main UI thread.
     *
     * TODO: retry logic will not exist on the consumer-side of this method, so rather than
     * NetworkResult, another result<->error optional type should be used instead.
     */
    fun getImageByUrl(
        url: URL
    ): Publisher<NetworkResult<Bitmap>>
}

interface ImageOptimizationServiceInterface {
    /**
     * Take a given background and return a URI and image configuration that may be used to display
     * it efficiently.  It may perform transforms on the URI and background image configuration to
     * cut down retrieving and decoding an unnecessary larger image than needed for the context.
     *
     * Note that this does not actually perform any sort optimization operation locally.
     *
     * @return The optimized image configuration, which includes the URI with optimization
     * parameters.  May be null if the background in question has no image.
     */
    fun optimizeImageBackground(
        background: Background,
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics
    ): OptimizedImage?

    /**
     * Take a given image block and return the URI with optimization parameters needed to display
     * it.
     *
     * @return optimized URI.  May be null if the image block in question has no image set.
     */
    fun optimizeImageBlock(
        block: ImageBlock,
        targetViewPixelSize: PixelSize,
        displayMetrics: DisplayMetrics
    ): URI?
}

/**
 * A retrieval URI and configuration needed for displaying an image.
 */
data class OptimizedImage(
    /**
     * The (potentially) modified URI.
     */
    val uri: URI,

    /**
     * The (potentially) modified image display configuration.
     */
    val imageConfiguration: BackgroundImageConfiguration
)
