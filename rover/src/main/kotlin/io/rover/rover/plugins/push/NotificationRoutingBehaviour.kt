package io.rover.rover.plugins.push

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.userexperience.experience.containers.StandaloneExperienceHostActivity

/**
 *
 */
class NotificationActionRoutingBehaviour(
    private val applicationContext: Context
): NotificationActionRoutingBehaviourInterface {

    override fun notificationActionToIntent(action: PushNotificationAction): Intent {
        return when(action) {
            is PushNotificationAction.PresentExperience ->
                StandaloneExperienceHostActivity.makeIntent(applicationContext, action.experienceId)
            is PushNotificationAction.PresentWebsite ->
                // Note: PresentWebsite URIs come from a trusted source, that is, the app's owner
                // commanding a pushNotification through Rover.  Non-web URI schemes are filtered
                // out, as well.
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url.toString()))
            is PushNotificationAction.OpenUrl ->
                // Like above, but non-web URI schemes are not being filtered.
                Intent(Intent.ACTION_VIEW, Uri.parse(action.url.toString()))
        }
    }
}
