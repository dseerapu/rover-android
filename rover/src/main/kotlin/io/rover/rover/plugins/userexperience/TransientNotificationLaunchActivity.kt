package io.rover.rover.plugins.userexperience

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.push.NotificationContentPendingIntentSynthesizer

/**
 * When the user taps a Rover notification created for the app by
 * [NotificationContentPendingIntentSynthesizer] in the Android notification tray, we want an
 * analytics event to be emitted as a side-effect.  However, the target screen could be either an
 * external application (particularly, a web browser) or some other Activity in the app that would
 * be difficult to instrument.
 *
 * So, this Activity will be responsible for emitting that that side-effect happens, although
 * it does so by delegating to [NotificationOpen].
 */
class TransientNotificationLaunchActivity: AppCompatActivity() {
    private val notificationOpen by lazy {
        Rover.sharedInstance.openNotification
    }

    // TODO: make transparent/invisible somehow to avoid flicker

    override fun onStart() {
        super.onStart()

        log.v("Transient notification launch activity running.")

        // grab the notification back out of the arguments.
        val notificationJson = this.intent.extras.getString(NOTIFICATION_JSON)

        // this will also do the side-effect of issuing the Notification Opened event, which
        // is the whole reason for this activity existing.
        val intentStack = notificationOpen.intentStackForOpeningNotificationFromNotificationsDrawer(notificationJson)

        ContextCompat.startActivities(
            this,
            intentStack.toTypedArray()
        )
        finish()
    }

    companion object {
        fun generateLaunchIntent(
            context: Context,
            wireEncoder: WireEncoderInterface,
            notification: Notification
        ): PendingIntent {
            val notificationJson = wireEncoder.encodeNotification(notification)

            return PendingIntent.getActivity(
                context,
                // use hashcode on the ID string (itself a UUID, which is bigger than 32 bits alas)
                // as a way of keeping the separate PendingIntents actually separate.  chance of
                // collision is not too high.
                notification.id.hashCode(),
                Intent(
                    context,
                    TransientNotificationLaunchActivity::class.java
                ).apply {
                    putExtra(
                        NOTIFICATION_JSON, notificationJson.toString()
                    )
                },
                PendingIntent.FLAG_ONE_SHOT
            )
        }

        private const val NOTIFICATION_JSON = "notification_json"
    }
}