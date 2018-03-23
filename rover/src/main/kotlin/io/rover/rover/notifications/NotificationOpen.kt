package io.rover.rover.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.rover.rover.core.data.domain.AttributeValue
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.events.EventQueueService
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.events.domain.Event
import io.rover.rover.core.logging.log
import io.rover.rover.experiences.TransientNotificationLaunchActivity
import org.json.JSONObject

/**
 * Open a notification by executing its [PushNotificationAction].
 */
open class NotificationOpen(
    private val applicationContext: Context,
    private val wireEncoder: WireEncoderInterface,
    private val eventsService: EventQueueServiceInterface,
    private val routingBehaviour: ActionRoutingBehaviourInterface,
    private val notificationContentPendingIntentSynthesizer: NotificationContentPendingIntentSynthesizerInterface
): NotificationOpenInterface {

    override fun pendingIntentForAndroidNotification(notification: Notification): PendingIntent {
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

        val intentAndBackstackRequest = routingBehaviour.actionUriToIntent(notification.uri)

        return if(intentAndBackstackRequest.noBackstack && intentAndBackstackRequest.intent != null) {
            listOf(intentAndBackstackRequest.intent)
        } else {
            log.v("SYNTHESIZING REQUEST FOR NOTIFICATION")
            notificationContentPendingIntentSynthesizer.synthesizeNotificationIntentStack(
                intentAndBackstackRequest.intent,
                notification.isNotificationCenterEnabled
            ).apply { log.v("... CAME OUT AS $this")}

        }
    }

    override fun intentForOpeningNotificationDirectly(notification: Notification): Intent? {
        // we only want to open the given notification's action in the case where it would
        // navigate somewhere useful, not just re-open the app.
        return if (routingBehaviour.isDirectOpenAppropriate(notification.uri)) {
            // side-effect: issue open event.
            issueNotificationOpenedEvent(
                notification,
                NotificationSource.NotificationCenter
            )

            routingBehaviour.actionUriToIntent(notification.uri).intent
        } else null
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
