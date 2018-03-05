package io.rover.rover.plugins.userexperience

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import io.rover.rover.Rover
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.http.WireEncoderInterface

/**
 * When the user taps a Rover notification created for the app by
 * [NotificationContentPendingIntentSynthesizer], we want an analytics event to be emitted as a
 * side-effect.  However, the target screen could be either an external application (particularly, a
 * web browser) or some other Activity in the app that would be difficult to instrument.
 *
 * So, this Activity will be
 */

// points of investigation:

// where should the intent synthesis happen now?  should it shuttled through the notification and
// eventually to here as a value object (if that's even possible), or should we instead evaluate the
// notification here, and synthesize the intent with the auto-generated backstack right here (if it
// is possible to do such a replacement of an existing Task with a synthesized backstack intent)?

// replacing: https://stackoverflow.com/questions/2116158/replace-current-activity

// marshalling built task intent through notification as an extra? actually, wait, probably not
// worth doing that because to emit the event we're going to want the whole notification anyway.  So
// let's just shuttle the entire notification.  This also removes the risk of pendingintents being
// collapsed.


class TransientNotificationLaunchActivity: AppCompatActivity() {
    private val openNotification by lazy {
        Rover.sharedInstance.notificationOpen
    }

    // TODO: make transparent/invisible somehow to avoid flicker

    override fun onStart() {
        super.onStart()

        // grab the notification back out of the arguments.
        val notificationJson = this.intent.extras.getString(NOTIFICATION_JSON)

        ContextCompat.startActivities(
            this,
            openNotification.intentStackForImmediateNotificationAction(notificationJson).toTypedArray()
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