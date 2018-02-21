package io.rover.rover.plugins.userexperience

import android.content.Intent
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

/**
 * Responsible for generating Intents that will route the user to the appropriate place in your app
 * for viewing Rover-mediated content.
 *
 * A default implementation is provided by [DefaultTopLevelNavigation], which uses the standalone
 * activities bundled along with the Rover SDK.  However, you will need your own implementation of
 * [TopLevelNavigation] in your application if you wish to host either the ExperienceView or
 * NavigationCenterView directly in your own Activities.
 */
interface TopLevelNavigation {
    /**
     * Generate an Intent for displaying an Experience.
     */
    fun displayExperienceIntent(experienceId: String): Intent

    /**
     * Generate an Intent for navigating your app to the Notification Center.
     *
     * For example, if you host the Notification Center within the Settings area of your app, and
     * your app is a single-Activity app or otherwise using some sort of custom routing arrangement
     * (such as Fragments or Conductor), then you will need to make the Intent address the
     * appropriate activity, and command it with arguments to navigate to the appropriate place.
     */
    fun displayNotificationCenterIntent(): Intent

    fun mainScreenIntent

        // TODO ANDREW START HERE
}