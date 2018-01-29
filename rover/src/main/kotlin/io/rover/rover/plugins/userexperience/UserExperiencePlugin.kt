package io.rover.rover.plugins.userexperience

import io.rover.rover.Rover
import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface

/**
 * This is the Rover User Experience plugin.  It contains the entire Rover Experiences system.
 *
 * To use it to your project, add [UserExperiencePluginAssembler] to your [Rover.initialize].
 *
 * @param components These are all the internal dependencies needed by the Rover experiences plugin.
 * The assembler should construct one of these.  Some of these components allow for you to override
 * them, and thus if you like you can create a your own subclass of
 * [UserExperiencePluginComponentsInterface] (or, if in Kotlin, use class delegation if you prefer)
 * and provide either a subclass of the given component you wish to override or even your own
 * implementation of the interface.
 */
class UserExperiencePlugin(
    components: UserExperiencePluginComponentsInterface
) : UserExperiencePluginInterface, ViewModelFactoryInterface by components.stockViewModelFactory
