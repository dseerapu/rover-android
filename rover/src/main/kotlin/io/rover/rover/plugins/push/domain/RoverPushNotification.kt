package io.rover.rover.plugins.push.domain

import io.rover.rover.plugins.push.PushPluginAssembler
import java.net.URL

/**
 * Rover push notifications have this format.
 *
 * When received from Firebase, they are delivered as a JSON-encoded object set as the `message` key
 * on the Firebase `RemoteMessage`.
 *
 */
interface RoverPushNotification {
    /**
     * An Android channel ID.  If not set, Rover will use the default channel id set for the whole
     * Push Plugin (see [PushPluginAssembler]).
     */
    val channelId: String?
    val title: String
    val text: String
    val read: Boolean
    val contentType: String

    companion object
}

data class WebsitePushNotification(
    override val channelId: String?,
    override val title: String,
    override val text: String,
    override val read: Boolean,
    override val contentType: String,
    val websiteUrl: URL
): RoverPushNotification {
    companion object
}

data class DeepLinkPushNotification(
    override val channelId: String?,
    override val title: String,
    override val text: String,
    override val read: Boolean,
    override val contentType: String,
    val deepLinkUrl: URL
): RoverPushNotification {
    companion object
}

data class ExperiencePushNotification(
    override val channelId: String?,
    override val title: String,
    override val text: String,
    override val read: Boolean,
    override val contentType: String,
    val experienceId: String
): RoverPushNotification {
    companion object
}
