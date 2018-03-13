package io.rover.rover.plugins.userexperience

import android.content.Context
import android.util.DisplayMetrics
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.Container
import io.rover.rover.core.container.Resolver
import io.rover.rover.core.container.Scope
import io.rover.rover.platform.DateFormatting
import io.rover.rover.platform.IoMultiplexingExecutor
import io.rover.rover.platform.LocalStorage
import io.rover.rover.platform.SharedPreferencesLocalStorage
import io.rover.rover.core.data.DataPluginInterface
import io.rover.rover.core.data.graphql.WireEncoder
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.notifications.NotificationActionRoutingBehaviour
import io.rover.rover.notifications.NotificationContentPendingIntentSynthesizer
import io.rover.rover.core.assets.AndroidAssetService
import io.rover.rover.core.assets.AssetService
import io.rover.rover.core.assets.ImageDownloader
import io.rover.rover.core.assets.ImageOptimizationService
import io.rover.rover.core.assets.ImageOptimizationServiceInterface
import io.rover.rover.experiences.*
import io.rover.rover.experiences.ui.StockViewModelFactory
import io.rover.rover.experiences.ui.ViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.blocks.BlockViewModelFactoryInterface
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.AndroidRichTextToSpannedTransformer
import io.rover.rover.plugins.userexperience.experience.blocks.concerns.text.RichTextToSpannedTransformer
import java.util.concurrent.Executor

open class UserExperiencePluginComponents(
    protected val displayMetrics: DisplayMetrics,
    protected val dataPluginInterface: DataPluginInterface,
    protected val eventsPlugin: EventQueueServiceInterface,
    protected val applicationContext: Context
) : UserExperiencePluginComponentsInterface {
    override val stockViewModelFactory: ViewModelFactoryInterface by lazy {
        StockViewModelFactory(
            blockViewModelFactory,
            dataPluginInterface,
            eventsPlugin,
            ioExecutor,
            localStorage
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

    private val localStorage: LocalStorage by lazy {
        SharedPreferencesLocalStorage(applicationContext)
    }
}

/**
 * Pass an instance of this to Rover.initialize() to opt into using the User Experiences Rover
 * plugin, which enables support for Rover Experiences.
 */
class UserExperiencePluginAssembler(
    private val applicationContext: Context
): Assembler {
    override fun assemble(container: Container) {
        container.register(Scope.Singleton, UserExperiencePluginInterface::class.java, null) { resolver: Resolver ->
            UserExperiencePlugin(
                UserExperiencePluginComponents(
                    applicationContext.resources.displayMetrics,
                    resolver.resolve(DataPluginInterface::class.java) ?: throw RuntimeException(
                        "The User Experience Plugin requires the Data Plugin.  Make sure you have the Data Plugin added to the Assemblers list in Rover.initialize()."
                    ),
                    resolver.resolve(EventQueueServiceInterface::class.java) ?: throw RuntimeException(
                        "The User Experience Plugin requires the Events Plugin.  Make sure you have the Events Plugin added to the Assemblers list in Rover.initialize()."
                    ),
                    applicationContext
                )
            )
        }

        // TODO all of what follows is wrong.
        container.register(Scope.Singleton, NotificationOpenInterface::class.java) { resolver: Resolver ->
            val topnav = DefaultTopLevelNavigation(applicationContext)
            val routingBehaviour = NotificationActionRoutingBehaviour(applicationContext, topnav)

            NotificationOpen(
                applicationContext,
                WireEncoder(DateFormatting()),
                resolver.resolveSingletonOrFail(EventQueueServiceInterface::class.java),
                routingBehaviour,
                NotificationContentPendingIntentSynthesizer(
                    applicationContext, topnav, routingBehaviour
                )
            )
        }
    }
}
