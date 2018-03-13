package io.rover.rover

import android.content.Context
import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.ContainerResolver
import io.rover.rover.core.container.PluginContainer
import io.rover.rover.core.logging.LogEmitter
import io.rover.rover.core.data.http.AsyncTaskAndHttpUrlConnectionNetworkClient
import io.rover.rover.core.data.DataPlugin
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.notifications.NotificationHandlerInterface
import io.rover.rover.experiences.NotificationOpenInterface
import io.rover.rover.experiences.UserExperiencePluginInterface
import java.net.HttpURLConnection

/**
 * Entry point for the Rover SDK.
 *
 * The Rover SDK consists of several discrete Plugins, which each offer a major vertical
 * (eg. Experiences, Location, and Events) of the Rover Platform.  It's up to you to select which
 * are appropriate to activate in your app.
 *
 * TODO: exhaustive usage information.
 *
 * Serves as a dependency injection container for the various components (Plugins) of the Rover SDK.
 */
class Rover(
    assemblers: List<Assembler>
): ContainerResolver by PluginContainer(assemblers) {

    val dataPlugin: DataPlugin
        get() = this.resolve(DataPlugin::class.java) ?: throw missingPluginError("DataPlugin")

    // TODO: these accessors will likely disappear entirely and instead be replaced with usage of a DI container.

    @Deprecated("Consumers will soon obtain view models from the DI container directly.")
    val userExperiencePlugin: UserExperiencePluginInterface
        get() = this.resolve(UserExperiencePluginInterface::class.java) ?: throw missingPluginError("UserExperiencePlugin")

    val eventQueue: EventQueueServiceInterface
        get() = this.resolve(EventQueueServiceInterface::class.java) ?: throw missingPluginError(("EventQueueService"))

    val notificationHandler: NotificationHandlerInterface
        get() = this.resolve(NotificationHandlerInterface::class.java) ?: throw missingPluginError(("NotificationHandler"))

    val logEmitter: LogEmitter
        get() = this.resolve(LogEmitter::class.java) ?: throw missingPluginError("LogEmitter")

    val notificationOpen: NotificationOpenInterface
        get() = this.resolve(NotificationOpenInterface::class.java) ?: throw missingPluginError("NotificationOpen")

    private fun missingPluginError(name: String): Throwable {
        throw RuntimeException("Logger not registered.  Did you include $name() in the assembler list?")
    }

    companion object {
        private var sharedInstanceBackingField: Rover? = null

        // we have a global singleton of the Rover container.
        @JvmStatic
        val sharedInstance: Rover
            get() = sharedInstanceBackingField ?: throw RuntimeException("Rover shared instance accessed before calling initialize.\n\n" +
                "Did you remember to call Rover.initialize() in your Application.onCreate()?")

        @JvmStatic
        fun initialize(vararg assemblers: Assembler) {
            val rover = Rover(assemblers.asList())
            if(sharedInstanceBackingField != null) {
                throw RuntimeException("Rover already initialized.  This is most likely a bug.")
            }
            sharedInstanceBackingField = rover
        }

        /**
         * Be sure to always call this after [Rover.initialize] in your Application's onCreate()!
         *
         * Rover internally uses the standard HTTP client included with Android, but to work
         * effectively it needs HTTP caching enabled.  Unfortunately, this can only be done at the
         * global level, so we ask that you call this method -- [installSaneGlobalHttpCache] -- at
         * application start time (unless you have already added your own cache to Android's
         * [HttpURLConnection].
         */
        @JvmStatic
        fun installSaneGlobalHttpCache(applicationContext: Context) {
            AsyncTaskAndHttpUrlConnectionNetworkClient.installSaneGlobalHttpCache(applicationContext)
        }

        /**
         * If you wish to construct your own instance of [Rover], perhaps within your App's own
         * dependency injection system, then in lieu of calling [Rover.initialize] you may instead
         * instantiate your own [Rover] instance and register it here with
         * [registerCustomRoverInstance] so that it will become the globally shared
         * ([Rover.sharedInstance]) Rover instance.
         *
         * This is necessary because the Rover standalone Experience activity needs to be able to
         * discover its dependencies in the Rover SDK.
         *
         * If you're not sure if you need this method, then you probably don't.
         */
        @JvmStatic
        fun registerCustomRoverInstance(userProvidedRover: Rover) {
            if(sharedInstanceBackingField != null) {
                throw RuntimeException("Rover already initialized.  This is most likely a bug.")
            }
            sharedInstanceBackingField = userProvidedRover
        }
    }
}
