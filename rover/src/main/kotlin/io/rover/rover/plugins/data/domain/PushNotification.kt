package io.rover.rover.plugins.data.domain

import java.net.URI
import java.net.URL

/**
 * Rover push notifications have this format.
 *
 * Note that this is used not only in the GraphQL API but also in the push notification payloads
 * delivered over the push platform (typically FCM).
 *
 * When received from Firebase, they are delivered as a JSON-encoded object set as the `message` key
 * on the Firebase `RemoteMessage`.
 *
 */
data class PushNotification(
    /**
     * An Android channel ID.  If not set, Rover will use the default channel id set for the whole
     * Push Plugin (see [PushPluginAssembler]).
     */
    val id: String,
    val channelId: String?,
    val title: String,
    val text: String,
    val read: Boolean,
    val contentType: String,
    val isNotificationCenterEnabled: Boolean,
    val action: PushNotificationAction
) {
    companion object
}

sealed class PushNotificationAction {
    data class Website(
        val websiteUrl: URL
    ): PushNotificationAction()

    data class DeepLink(
        val deepLinkUrl: URI
    ): PushNotificationAction()

    data class Experience(
        val experienceId: String
    ): PushNotificationAction()

    companion object
}
