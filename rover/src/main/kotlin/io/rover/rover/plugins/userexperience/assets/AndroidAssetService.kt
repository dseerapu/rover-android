package io.rover.rover.plugins.userexperience.assets

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import io.rover.rover.plugins.data.NetworkResult
import io.rover.rover.plugins.data.http.NetworkTask
import java.net.URL
import java.util.concurrent.Executor

class AndroidAssetService(
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

    override fun getImageByUrl(
        url: URL,
        completionHandler: ((NetworkResult<Bitmap>) -> Unit)
    ): NetworkTask {

        return SynchronousOperationNetworkTask(
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
        private var cancelled = false

        override fun cancel() {
            cancelled = true
        }

        private fun execute() {
            val result = try {
                doSynchronousWorkload()
            } catch (e: Throwable) {
                emitError(e)
                return
            }

            if (!cancelled) {
                emitResult(result)
            }
        }

        override fun resume() {
            executor.execute {
                execute()
            }
        }
    }
}
