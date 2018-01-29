package io.rover.rover.plugins.userexperience

import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface

interface UserExperiencePluginInterface : ViewModelFactoryInterface {
    // for now all consumers of the User Plugin only use it to construct the view models.
}

interface UserExperiencePluginComponentsInterface {
    /**
     * This is the stock view model factory.  It can manufacture the top-level Experience view
     * models.
     */
    val stockViewModelFactory: ViewModelFactoryInterface

    val measurementService: MeasurementService
}
