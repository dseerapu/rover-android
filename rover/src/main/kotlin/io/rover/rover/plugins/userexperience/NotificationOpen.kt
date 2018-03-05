package io.rover.rover.plugins.userexperience

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.rover.rover.plugins.data.domain.AttributeValue
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.events.domain.Event
import io.rover.rover.plugins.push.NotificationActionRoutingBehaviour
import io.rover.rover.plugins.push.NotificationContentPendingIntentSynthesizerInterface
import org.json.JSONObject

/**
 * Open a notification by executing its [PushNotificationAction].
 */
open class NotificationOpen(
    private val applicationContext: Context,
    private val wireEncoder: WireEncoderInterface,
    private val eventsService: EventsPluginInterface,
    private val routingBehaviour: NotificationActionRoutingBehaviour,
    private val notificationContentPendingIntentSynthesizer: NotificationContentPendingIntentSynthesizerInterface
): NotificationOpenInterface {
    override fun pendingIntentForAndroidNotification(notification: Notification): PendingIntent {

        notificationContentPendingIntentSynthesizer.synthesizeNotificationIntentStack(
            notification
        )

        return TransientNotificationLaunchActivity.generateLaunchIntent(
            applicationContext,
            wireEncoder,
            notification
        )
    }

    override fun intentStackForImmediateNotificationAction(notificationJson: String): List<Intent> {
        val notification = wireEncoder.decodeNotification(JSONObject(notificationJson))

        issueNotificationOpenedEvent(
            notification
        )

        return notificationContentPendingIntentSynthesizer.synthesizeNotificationIntentStack(
            notification
        )
    }

    override fun intentForDirectlyOpeningNotification(notification: Notification): Intent {
        return routingBehaviour.notificationActionToIntent(notification.action)
    }

    protected fun issueNotificationOpenedEvent(notification: Notification) {
        eventsService.trackEvent(
            Event(
                "Notification Opened",
                hashMapOf(
                    Pair("notificationID", AttributeValue.String(notification.id)),
                    Pair("source", AttributeValue.String(""))
                )
            )
        )
    }
}
