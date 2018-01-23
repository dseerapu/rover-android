package io.rover.rover.plugins.userexperience.experience

import io.rover.rover.UserExperiencePluginComponents

interface UserExperiencePluginInterface : ViewModelFactoryInterface {
    // various bits of functionality will go here.
}

/**
 *
 *
 * - you may override the view model creation methods.
 */
open class UserExperiencePlugin(
    userExperiencePluginComponents: UserExperiencePluginComponents
) : UserExperiencePluginInterface, ViewModelFactoryInterface by userExperiencePluginComponents.stockViewModelFactory {

}
