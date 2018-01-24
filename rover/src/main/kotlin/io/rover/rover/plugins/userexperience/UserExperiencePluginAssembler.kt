package io.rover.rover.plugins.userexperience

import android.annotation.SuppressLint
import android.os.Build
import android.util.DisplayMetrics
import io.rover.rover.UserExperiencePluginComponents
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.userexperience.assets.AndroidAssetService
import io.rover.rover.plugins.userexperience.assets.AssetService
import io.rover.rover.plugins.userexperience.assets.ImageDownloader
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationService
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationServiceInterface
import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.UserExperiencePlugin
import io.rover.rover.plugins.userexperience.experience.UserExperiencePluginInterface
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.RichTextToSpannedTransformer
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@SuppressLint("NewApi")
class LiveUserExperiencePluginComponents(
    private val displayMetrics: DisplayMetrics,
    private val dataPluginInterface: DataPluginInterface
) : UserExperiencePluginComponents {
    override val stockViewModelFactory: ViewModelFactoryInterface by lazy {
        StockViewModelFactory(
            measurementService,
            assetService,
            imageOptimizationService,
            dataPluginInterface
        )
    }

    override val measurementService: MeasurementService by lazy {
        AndroidMeasurementService(
            displayMetrics,
            richTextToSpannedTransformer
        )
    }

    private val richTextToSpannedTransformer: RichTextToSpannedTransformer by lazy {
        AndroidRichTextToSpannedTransformer()
    }

    private val assetService: AssetService by lazy {
        AndroidAssetService(imageDownloader, ioExecutor)
    }

    private val imageDownloader: ImageDownloader by lazy {
        ImageDownloader(ioExecutor)
    }

    /**
     * This is an [Executor] tuned for multiplexing I/O, not for computation.
     *
     * Avoid doing computation on it.
     */
    private val ioExecutor: Executor by lazy {
        val alwaysUseLegacyThreadPool = false

        val cpuCount = Runtime.getRuntime().availableProcessors()

        val useModernThreadPool = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !alwaysUseLegacyThreadPool

        log.v("Setting up ioExecutor Thread Pool.  Number of CPUs: $cpuCount. Using ${if(useModernThreadPool) "forkjoin" else "legacy"} executor.")

        if (useModernThreadPool) {
            // The below is equivalent to:
            // Executors.newWorkStealingPool(availableProcessors * 100)

            // It's specifically meant for use in a ForkJoinTask work-stealing workload, but as a
            // side-effect it also configures an Executor that does a fair job of enforcing a
            // maximum thread pool size, which is difficult to do with the stock Executors due to an
            // odd design decision by the Java design team a few decades ago:
            // https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
            ForkJoinPool(
                 cpuCount * 100,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true)
        } else {
            // we'll use an unbounded thread pool on Android versions older than 21.

            // while one would expect that you could set a maxPoolSize value and have sane
            // scale-up-and-down behaviour, that is not how it works out.  ThreadPoolExecutor
            // actually will only create new threads (up to the max), when the queue you give it
            // reports itself as being full by means of the [Queue.offer] interface.  A common
            // workaround is to use SynchronousQueue, which always reports itself as full.
            // Unfortunately, this workaround prohibits the thread pool maximum size being enforced.
            // However, on casual testing for our use case it appears that for our I/O multiplexing
            // thread pool we can largely get away with this.
            ThreadPoolExecutor(
                10,
                Int.MAX_VALUE,
                2,
                TimeUnit.SECONDS,
                SynchronousQueue<Runnable>()
            )
        }
    }

    private val imageOptimizationService: ImageOptimizationServiceInterface by lazy {
        ImageOptimizationService()
    }
}

class UserExperiencePluginAssembler(
    private val displayMetrics: DisplayMetrics
): Assembler {
    override fun register(container: Container) {
        container.register(UserExperiencePluginInterface::class.java) { resolver ->
            UserExperiencePlugin(
                LiveUserExperiencePluginComponents(
                    displayMetrics,
                    resolver.resolve(DataPluginInterface::class.java) ?: throw RuntimeException(
                        "The User Experience Plugin requires the Data Plugin.  Make sure you have the Data Plugin added to the Assemblers list in Rover.initialize()."
                    )
                )
            )
        }
    }
}

/**
 * The stock [ThreadPoolExecutor] in fact relies on the given work queue
 */
class ScalingThreadPoolExecutor() {

}