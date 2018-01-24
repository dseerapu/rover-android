package io.rover.rover.plugins.userexperience

import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface

interface UserExperiencePluginInterface : ViewModelFactoryInterface {

    // various bits of functionality to be exposed and overridable by the User Experience plugin
    // will go here.

}

interface UserExperiencePluginComponentsInterface {
    val stockViewModelFactory: ViewModelFactoryInterface

    val measurementService: MeasurementService
}
