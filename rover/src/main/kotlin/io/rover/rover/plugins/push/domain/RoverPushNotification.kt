package io.rover.rover.plugins.push.domain

import io.rover.rover.plugins.push.PushPluginAssembler

/**
 * Rover push notifications have this format.
 *
 * When received from Firebase, they are delivered as a JSON-encoded object set as the `message` key
 * on the Firebase `RemoteMessage`.
 *
 */
data class RoverPushNotification(
    /**
     * An Android channel ID.  If not set, Rover will use the default channel id set for the whole
     * Push Plugin (see [PushPluginAssembler]).
     */
    val channelId: String?,
    val title: String,
    val text: String,
    val read: Boolean,
    val contentType: String
) {
    companion object
}