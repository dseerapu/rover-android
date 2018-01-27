package io.rover.rover.plugins.userexperience

import io.rover.rover.plugins.userexperience.experience.StockViewModelFactory
import io.rover.rover.plugins.userexperience.experience.ViewModelFactoryInterface

interface UserExperiencePluginInterface : ViewModelFactoryInterface {
    // for now all consumers of the User Plugin only use it to construct the view models.
}

interface UserExperiencePluginComponentsInterface {
    /**
     * This is the stock version of view model factory.  It can manufacture all the view models,
     * specifically the top-level view models for containing the entire Experience, or the smaller
     * ones that own merely a single block.
     *
     * However, it must be not be provided to any of the other objects in the component, because the
     * [UserExperiencePlugin] allows for overriding the [ViewModelFactoryInterface], and as such
     * that hypothetical other component would not be exposed to those overrides.
     */
    val stockViewModelFactory: StockViewModelFactory

    val measurementService: MeasurementService
}
