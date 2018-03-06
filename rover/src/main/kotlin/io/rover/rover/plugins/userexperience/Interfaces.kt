package io.rover.rover.plugins.userexperience

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.content.ContextCompat
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.push.NotificationActionRoutingBehaviour
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

    fun openAppIntent(): Intent
}

interface NotificationOpenInterface {
    /**
     * A pending intent that will be used for the Android notification itself
     *
     * Will return a [PendingIntent] suitable for use as an Android notification target that will
     * launch the [TransientNotificationLaunchActivity] to start up and
     */
    fun pendingIntentForAndroidNotification(notification: Notification): PendingIntent

    /**
     * This is called by the transient notification launch activity to replace itself with a new
     * stack.
     *
     * The returned Intents should be started immediately with [ContextCompat.startActivities]. This
     * method in fact has a side-effect of dispatching an analytics Event.
     */
    fun intentStackForImmediateNotificationAction(notificationJson: String): List<Intent>

    /**
     * Return an intent for directly opening the notification.
     *
     * Note: if you wish to override the intent creation logic, instead considering overriding
     * [TopLevelNavigation] or [NotificationActionRoutingBehaviour].
     *
     * Returns null if no intent is appropriate.
     */
    fun intentForDirectlyOpeningNotification(notification: Notification): Intent?
}