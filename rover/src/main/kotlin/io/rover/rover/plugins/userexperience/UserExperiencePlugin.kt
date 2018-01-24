package io.rover.rover.plugins.userexperience

import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface

/**
 *
 *
 * - you may override the view model creation methods.
 */
open class UserExperiencePlugin(
    userExperiencePluginComponents: UserExperiencePluginComponentsInterface
) : UserExperiencePluginInterface, ViewModelFactoryInterface by userExperiencePluginComponents.stockViewModelFactory {

}
