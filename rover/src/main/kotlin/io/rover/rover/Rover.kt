package io.rover.rover

import io.rover.rover.core.container.Assembler
import io.rover.rover.core.container.ContainerResolver
import io.rover.rover.core.container.PluginContainer
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.data.DataPlugin
import io.rover.rover.plugins.userexperience.experience.UserExperiencePluginInterface

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

    val userExperiencePlugin: UserExperiencePluginInterface
        get() = this.resolve(UserExperiencePluginInterface::class.java) ?: throw missingPluginError("UserExperiencePlugin")

    private fun missingPluginError(name: String): Throwable {
        throw RuntimeException("Data Plugin not registered.  Did you include $name() in the assembler list?")
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
                log.w("Rover already initialized.  This is most likely a bug.")
            }
            sharedInstanceBackingField = rover
        }
    }
}
