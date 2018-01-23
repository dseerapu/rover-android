package io.rover.rover.plugins.userexperience

import android.util.DisplayMetrics
import io.rover.rover.UserExperiencePluginComponents
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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

    private val ioExecutor: ThreadPoolExecutor by lazy {
        ThreadPoolExecutor(
            10,
            Runtime.getRuntime().availableProcessors() * 20,
            2,
            TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>()
        )
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
