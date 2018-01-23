@file:JvmName("AssetRetrievalStage")

package io.rover.rover.plugins.userexperience.assets

import io.rover.rover.core.streams.blockForResult
import java.io.BufferedInputStream
import java.net.URL

/**
 * Stream the asset from a remote HTTP API.
 *
 * Be mindful that the stream downstream of this one closes the input streams once they are finished
 * reading from them.
 *
 * This never faults to anything further down in the pipeline; it always retrieves from the API.
 */
class AssetRetrievalStage(
    private val imageDownloader: ImageDownloader
) : SynchronousPipelineStage<URL, BufferedInputStream> {
    /**
     * Be sure to call [BufferedInputStream.close] once complete reading the stream.
     */
    override fun request(input: URL): PipelineStageResult<BufferedInputStream> {

        // so now I am going to just *block* while waiting for the callback, since this is all being
        // run on a background executor.
        val streamResult = imageDownloader
            .downloadStreamFromUrl(input)
            .blockForResult()
            .first()

        return when (streamResult) {
                is ImageDownloader.HttpClientResponse.ConnectionFailure -> {
                    PipelineStageResult.Failed(
                        RuntimeException("Network or HTTP error downloading asset", streamResult.reason)
                    )
                }
                is ImageDownloader.HttpClientResponse.ApplicationError -> {
                    PipelineStageResult.Failed(
                        RuntimeException("Remote HTTP API error downloading asset (code ${streamResult.responseCode}): ${streamResult.reportedReason}")
                    )
                }
                is ImageDownloader.HttpClientResponse.Success -> {
                    // we have the stream! pass it downstream for decoding.
                    PipelineStageResult.Successful(
                        streamResult.bufferedInputStream
                    )
                }
            }
    }
}