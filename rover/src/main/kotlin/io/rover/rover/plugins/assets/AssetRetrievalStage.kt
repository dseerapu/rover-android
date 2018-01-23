@file:JvmName("AssetRetrievalStage")

package io.rover.rover.plugins.assets

import io.rover.rover.plugins.data.HttpClientResponse
import io.rover.rover.plugins.data.HttpRequest
import io.rover.rover.plugins.data.HttpVerb
import io.rover.rover.plugins.data.NetworkClient
import io.rover.rover.plugins.data.NetworkTask
import java.io.BufferedInputStream
import java.net.URL
import java.util.concurrent.CountDownLatch

/**
 * Stream the asset from a remote HTTP API.
 *
 * This never faults to anything further down in the pipeline; it always retrieves from the API.
 */
class AssetRetrievalStage(
    private val networkClient: NetworkClient
) : SynchronousPipelineStage<URL, BufferedInputStream> {
    override fun request(input: URL): PipelineStageResult<BufferedInputStream> {
        // so now I am going to just *block* while waiting for the callback.
        return blockWaitForNetworkTask { completionHandler ->
            networkClient.networkTask(
                HttpRequest(input, hashMapOf(), HttpVerb.GET),
                null,
                completionHandler
            )
        }
    }

    private fun blockWaitForNetworkTask(invocation: (completionHandler: (HttpClientResponse) -> Unit) -> NetworkTask): PipelineStageResult<BufferedInputStream> {
        val latch = CountDownLatch(1)
        var returnStream: PipelineStageResult<BufferedInputStream>? = null
        invocation { clientResponse ->
            returnStream = when (clientResponse) {
                is HttpClientResponse.ConnectionFailure -> {
                    PipelineStageResult.Failed(
                        RuntimeException("Network or HTTP error downloading asset", clientResponse.reason)
                    )
                }
                is HttpClientResponse.ApplicationError -> {
                    PipelineStageResult.Failed(
                        RuntimeException("Remote HTTP API error downloading asset (code ${clientResponse.responseCode}): ${clientResponse.reportedReason}")
                    )
                }
                is HttpClientResponse.Success -> {
                    PipelineStageResult.Successful(
                        clientResponse.bufferedInputStream
                    )
                }
            }
            latch.countDown()
        }.resume()
        // we rely on the network task to handle network timeout for us, so we'll just wait
        // patiently indefinitely here.
        latch.await()

        return returnStream!!
    }
}