package io.rover.rover.plugins.userexperience

import android.util.DisplayMetrics
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.userexperience.assets.AndroidAssetService
import io.rover.rover.plugins.userexperience.assets.AssetService
import io.rover.rover.plugins.userexperience.assets.ImageDownloader
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationService
import io.rover.rover.plugins.userexperience.assets.ImageOptimizationServiceInterface
import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.RichTextToSpannedTransformer
import java.util.concurrent.Executor

open class UserExperiencePluginComponents(
    protected val displayMetrics: DisplayMetrics,
    protected val dataPluginInterface: DataPluginInterface,
    protected val eventsPlugin: EventsPluginInterface
) : UserExperiencePluginComponentsInterface {
    override val stockViewModelFactory: ViewModelFactoryInterface by lazy {
        StockViewModelFactory(
            blockViewModelFactory,
            dataPluginInterface,
            eventsPlugin
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

    open val assetService: AssetService by lazy {
        AndroidAssetService(imageDownloader, ioExecutor)
    }

    open val imageDownloader: ImageDownloader by lazy {
        ImageDownloader(ioExecutor)
    }

    open val ioExecutor: Executor by lazy {
        IoMultiplexingExecutor.build("userexperience")
    }

    open val imageOptimizationService: ImageOptimizationServiceInterface by lazy {
        ImageOptimizationService()
    }

    open val blockViewModelFactory: BlockViewModelFactoryInterface by lazy {
        BlockViewModelFactory(
            measurementService,
            assetService,
            imageOptimizationService
        )
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
                    ),
                    resolver.resolve(EventsPluginInterface::class.java) ?: throw RuntimeException(
                        "The User Experience Plugin requires the Events Plugin.  Make sure you have the Events Plugin added to the Assemblers list in Rover.initialize()."
                    )
                )
            )
        }
    }
}
