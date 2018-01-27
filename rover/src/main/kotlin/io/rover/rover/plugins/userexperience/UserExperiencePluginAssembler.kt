package io.rover.rover.plugins.userexperience

import android.util.DisplayMetrics
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.userexperience.assets.AndroidAssetService
import io.rover.rover.plugins.userexperience.assets.AssetService
import io.rover.rover.plugins.userexperience.assets.ImageDownloader
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationService
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationServiceInterface
import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.RichTextToSpannedTransformer
import java.util.concurrent.Executor

open class UserExperiencePluginComponents(
    private val displayMetrics: DisplayMetrics,
    private val dataPluginInterface: DataPluginInterface
) : UserExperiencePluginComponentsInterface {
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

    private val ioExecutor: Executor by lazy {
        IoMultiplexingExecutor.build("userexperience")
    }

    private val imageOptimizationService: ImageOptimizationServiceInterface by lazy {
        ImageOptimizationService()
    }
}

/**
 * Pass an instance of this to Rover.initialize() to opt into using the User Experiences Rover
 * plugin, which enables support for Rover Experiences.
 */
class UserExperiencePluginAssembler(
    private val displayMetrics: DisplayMetrics
): Assembler {
    override fun register(container: Container) {
        container.register(UserExperiencePluginInterface::class.java) { resolver ->
            UserExperiencePlugin(
                UserExperiencePluginComponents(
                    displayMetrics,
                    resolver.resolve(DataPluginInterface::class.java) ?: throw RuntimeException(
                        "The User Experience Plugin requires the Data Plugin.  Make sure you have the Data Plugin added to the Assemblers list in Rover.initialize()."
                    )
                )
            )
        }
    }
}
