package io.rover.rover.core.assets

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.CallbackReceiver
import io.rover.rover.core.streams.Publisher
import io.rover.rover.core.streams.asPublisher
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.core.data.http.NetworkTask
import java.net.URL
import java.util.concurrent.Executor

internal class AndroidAssetService(
    imageDownloader: ImageDownloader,
    private val ioExecutor: Executor
) : AssetService {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val synchronousImagePipeline = BitmapWarmGpuCacheStage(
        InMemoryBitmapCacheStage(
            DecodeToBitmapStage(
                AssetRetrievalStage(
                    imageDownloader
                )
            )
        )
    )

    /**
     * Fetch and process images using the pipeline.  We'll use [SynchronousOperationNetworkTask] to
     * do the CPU-bound processing.
     */
    private fun synchronousNetworkTaskForImageDownload(
        url: URL,
        completionHandler: ((NetworkResult<Bitmap>) -> Unit)
    ): NetworkTask {
        // TODO: we're doing Publisher based I/O and Publisher-based delivery, but we're using
        // NetworkTask for CPU processing. implement what's needed to do executor-based CPU
        // processing with Publisher and switch to that.

        return SynchronousOperationNetworkTask(
            // TODO: use a separate executor for this CPU-bound processing.
            ioExecutor,
            {
                // this block will be dispatched onto the ioExecutor by
                // SynchronousOperationNetworkTask.

                // ioExecutor is really only intended for I/O multiplexing only: it spawns many more
                // threads than CPU cores.  However, I'm bending that rule a bit by having image
                // decoding occur inband.  Thankfully, the risk of that spamming too many CPU-bound
                // workloads across many threads is mitigated by the HTTP client library
                // (HttpURLConnection, itself internally backed by OkHttp inside the Android
                // standard library) limiting concurrent image downloads from the same origin, which
                // most of the images in Rover experiences will be.
                synchronousImagePipeline.request(url)
            },
            { pipelineResult ->
                when (pipelineResult) {
                    is PipelineStageResult.Successful -> {
                        mainThreadHandler.post {
                            completionHandler(
                                NetworkResult.Success(pipelineResult.output)
                            )
                        }
                    }
                    is PipelineStageResult.Failed -> {
                        mainThreadHandler.post {
                            completionHandler(
                                NetworkResult.Error(pipelineResult.reason, false)
                            )
                        }
                    }
                }
            },
            { error ->
                mainThreadHandler.post {
                    completionHandler(
                        NetworkResult.Error(error, false)
                    )
                }
            }
        )
    }

    override fun getImageByUrl(
        url: URL
    ): Publisher<NetworkResult<Bitmap>> {
        // adapt the SynchronousOperationNetworkTask to Publisher:
        return { callback: CallbackReceiver<NetworkResult<Bitmap>> -> synchronousNetworkTaskForImageDownload(url, callback) }.asPublisher()
    }

    /**
     * A encapsulate a synchronous operation to an executor, yielding its result to the given
     * callback ([emitResult])
     */
    class SynchronousOperationNetworkTask<T>(
        private val executor: Executor,
        private val doSynchronousWorkload: () -> T,
        private val emitResult: (T) -> Unit,
        private val emitError: (Throwable) -> Unit
    ) : NetworkTask {
        @field:Volatile
        private var cancelled = false

        override fun cancel() {
            synchronized(cancelled) {
                cancelled = true
            }
        }

        private fun execute() {
            val result = try {
                doSynchronousWorkload()
            } catch (e: Throwable) {
                emitError(e)
                return
            }

            val cancelledValue = synchronized(cancelled) {
                cancelled
            }
            if (!cancelledValue) {
                emitResult(result)
            } else {
                log.v("Inhibited result delivery because synchronous operation network task cancelled.")
            }
        }

        override fun resume() {
            executor.execute {
                execute()
            }
        }
    }
}
