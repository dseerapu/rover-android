package io.rover.rover.plugins.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.TaskStackBuilder
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.userexperience.TopLevelNavigation
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity

/**
 *
 */
open class NotificationActionRoutingBehaviour(
    private val applicationContext: Context,
    private val topLevelNavigation: TopLevelNavigation
): NotificationActionRoutingBehaviourInterface {

    override fun notificationActionToIntent(action: PushNotificationAction): Intent {
        return when(action) {
            is PushNotificationAction.PresentExperience ->
                topLevelNavigation.displayExperienceIntent(action.experienceId)
            is PushNotificationAction.PresentWebsite ->
                // Note: PresentWebsite URIs come from a trusted source, that is, the app's owner
                // commanding a pushNotification through Rover.  Non-web URI schemes are filtered
                // out, as well.
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url.toString()))
            is PushNotificationAction.OpenUrl ->
                // Like above, but non-web URI schemes are not being filtered.
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url.toString()))
            is PushNotificationAction.OpenApp ->
                topLevelNavigation.openAppIntent()
        }
    }
}

interface NotificationContentPendingIntentSynthesizerInterface {
    fun synthesizePending(notification: Notification): PendingIntent
}

class NotificationContentPendingIntentSynthesizer(
    private val applicationContext: Context,
    private val topLevelNavigation: TopLevelNavigation,
    private val notificationActionRoutingBehaviour: NotificationActionRoutingBehaviourInterface
): NotificationContentPendingIntentSynthesizerInterface {
    override fun synthesizePending(notification: Notification): PendingIntent {
        val targetIntent = notificationActionRoutingBehaviour.notificationActionToIntent(notification.action)

        // now to synthesize the backstack.
        return TaskStackBuilder.create(applicationContext).apply {
            if(notification.isNotificationCenterEnabled) {
                // inject the Notification Centre for the user's app. TODO: allow user to *configure*
                // what their notification centre is, either with a custom URI template method OR
                // just with a meta-property in their Manifest. but by default we can bundle an Activity that hosts NotificationCentreView, I think.

                // for now, we'll just put some sort of
                addNextIntent(topLevelNavigation.displayNotificationCenterIntent())
            } else {
                // Instead of displaying the notification centre, display the parent activity the user set
                addNextIntent(topLevelNavigation.openAppIntent())
            }

            // so, targetIntent, since it uses extra data to pass arguments, might be a problem:
            // PendingIntents are value objects, but they do not fully encapsulate any extras data,
            // so they may find themselves "merged".  However, perhaps TaskStackBuilder is handling
            // this problem.
            addNextIntent(targetIntent)
        }.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)!!
    }
}