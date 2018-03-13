package io.rover.rover.core.data.domain

import java.net.URI
import java.net.URL
import java.util.Date

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
data class Notification(
    val id: String,

    /**
     * An Android channel ID.  If not set, Rover will use the default channel id set for the whole
     * Push Plugin (see [PushPluginAssembler]).
     */
    val channelId: String?,

    val title: String?,
    val body: String,

    /**
     * Has this notification been read?
     *
     * (when received over push this will always be false)
     */
    val isRead: Boolean,

    /**
     * Has this notification been deleted?
     *
     * When received over push will always be false.
     *
     * However, note that, for now, this will always be false when being returned from the device
     * state GraphQL API as well, although the API reserves the possibility of setting it to be
     * true in the future.
     */
    val isDeleted: Boolean,

    val isNotificationCenterEnabled: Boolean,

    val expiresAt: Date?,

    val deliveredAt: Date,

    val action: PushNotificationAction,

    val attachment: NotificationAttachment?
) {
    companion object
}

sealed class PushNotificationAction {
    data class PresentWebsite(
        val url: URL
    ): PushNotificationAction()

    data class OpenUrl(
        val url: URI
    ): PushNotificationAction()

    data class PresentExperience(
        val experienceId: String
    ): PushNotificationAction()

    class OpenApp: PushNotificationAction()

    companion object
}

sealed class NotificationAttachment(
    val typeName: String,
    val url: URL
) {
    class Audio(url: URL): NotificationAttachment("audio", url)
    class Image(url: URL): NotificationAttachment("image", url)
    class Video(url: URL): NotificationAttachment("video", url)

    override fun toString(): String {
        return "NotificationAttachment(typeName=$typeName, url=$url)"
    }

    companion object
}