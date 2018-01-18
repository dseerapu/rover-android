package io.rover.rover.services.assets

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import io.rover.rover.services.network.NetworkClient
import io.rover.rover.services.network.NetworkResult
import io.rover.rover.services.network.NetworkTask
import java.net.URL
import java.util.concurrent.Executor

class AndroidAssetService(
    networkClient: NetworkClient,
    private val ioExecutor: Executor
) : AssetService {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val synchronousImagePipeline = BitmapWarmGpuCacheStage(
        InMemoryBitmapCacheStage(
            DecodeToBitmapStage(
                AssetRetrievalStage(
                    networkClient
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
            { synchronousImagePipeline.request(url) },
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
