package io.rover.rover.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.rover.rover.core.data.domain.AttributeValue
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.domain.PushNotificationAction
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.events.EventQueueService
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.domain.Event
import io.rover.rover.experiences.TransientNotificationLaunchActivity
import org.json.JSONObject

/**
 * Open a notification by executing its [PushNotificationAction].
 */
open class NotificationOpen(
    private val applicationContext: Context,
    private val wireEncoder: WireEncoderInterface,
    private val eventsService: EventQueueServiceInterface,
    private val routingBehaviour: NotificationActionRoutingBehaviourInterface,
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

    override fun intentStackForOpeningNotificationFromNotificationsDrawer(notificationJson: String): List<Intent> {
        // side-effect: issue open event.
        val notification = wireEncoder.decodeNotification(JSONObject(notificationJson))

        issueNotificationOpenedEvent(
            notification,
            NotificationSource.Push
        )

        return notificationContentPendingIntentSynthesizer.synthesizeNotificationIntentStack(
            notification
        )
    }

    override fun intentForOpeningNotificationDirectly(notification: Notification): Intent? {
        // side-effect: issue open event.
        issueNotificationOpenedEvent(
            notification,
            NotificationSource.NotificationCenter
        )

        return when(notification.action) {
            // it is nonsensical to open the app when the app is already open, so this should be a no-op.
            is PushNotificationAction.OpenApp -> null
            else -> routingBehaviour.notificationActionToIntent(notification.action)
        }
    }

    protected fun issueNotificationOpenedEvent(notification: Notification, source: NotificationSource) {
        eventsService.trackEvent(
            Event(
                "Notification Opened",
                hashMapOf(
                    Pair("notificationID", AttributeValue.String(notification.id)),
                    Pair("source", AttributeValue.String(source.wireValue))
                )
            ),
            EventQueueService.ROVER_NAMESPACE
        )
    }

    enum class NotificationSource(val wireValue: String) {
        NotificationCenter("notificationCenter"), Push("push")
    }
}
