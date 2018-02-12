package io.rover.rover.plugins.push

import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.data.graphql.putProp
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.push.domain.DeepLinkPushNotification
import io.rover.rover.plugins.push.domain.ExperiencePushNotification
import io.rover.rover.plugins.push.domain.RoverPushNotification
import io.rover.rover.plugins.push.domain.WebsitePushNotification
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

class PushPlugin(
    private val applicationContext: Context,

    // TODO change to private val pushTokenTransmissionChannel: PushTokenTransmissionChannel,
    private val eventsPlugin: EventsPluginInterface,

    /**
     * A small icon is necessary for Android push notifications.  Pass a resid.
     *
     * Android design guidelines suggest that you use a multi-level drawable for your application
     * icon, such that you can specify one of its levels that is most appropriate as a single-colour
     * silhouette that can be used in the Android notification drawer.
     */
    @param:DrawableRes
    private val smallIconResId: Int,

    /**
     * The drawable level of [smallIconResId] that should be used for the icon silhouette used in
     * the notification drawer.
     */
    private val smallIconDrawableLevel: Int = 0,

    /**
     * This Channel Id will be used for any push notifications arrive without an included Channel
     * Id.
     */
    private val defaultChannelId: String? = null
): PushPluginInterface {

    override fun onTokenRefresh(token: String?) {
        // so, we need the token to be consumable from a FirebasePushTokenContextProvider
        eventsPlugin.setPushToken(token)
    }

    override fun onMessageReceivedData(parameters: Map<String, String>) {
        // if we have been called, then:
        // a) the notification does not have a display message component; OR
        // b) the app is running in foreground.

        if(!parameters.containsKey("message")) {
            log.w("Invalid push notification received: `message` data parameter not present. Possibly was a Display-only push notification. Ignoring.")
        }

        val message = parameters["message"] ?: return

        val (pushNotification, id) = try {
            val messageObject = JSONObject(message)
            val attributes = messageObject.getJSONObject("attributes")
            val id = messageObject.getInt("id")
            Pair(RoverPushNotification.Companion.decodeJson(attributes), id)
        } catch (e: JSONException) {
            log.w("Invalid push notification received: `$message`, resulting in '${e.message}'. Ignoring.")
            return
        } catch (e: MalformedURLException) {
            log.w("Invalid push notification received: `$message`, resulting in '${e.message}'. Ignoring.")
            return
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, pushNotification.channelId ?: defaultChannelId ?: NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(applicationContext)
        }

        builder.setContentTitle(pushNotification.title)
        builder.setContentText(pushNotification.text)
        builder.setSmallIcon(smallIconResId, smallIconDrawableLevel)

        notificationManager.notify(id, builder.build())
    }

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
}

internal fun RoverPushNotification.asJson(): JSONObject {
    return JSONObject().apply {
        listOf(
            RoverPushNotification::title,
            RoverPushNotification::text,
            RoverPushNotification::channelId,
            RoverPushNotification::contentType,
            RoverPushNotification::read
        ).forEach { putProp(this@asJson, it) }
    }
}

internal fun WebsitePushNotification.Companion.decodeJson(json: JSONObject): RoverPushNotification {
    return WebsitePushNotification(
        title = json.getString("android-title"),
        text = json.getString("notification-text"),
        channelId = json.optString("channel-id"),
        contentType = json.getString("content-type"),
        read = json.getBoolean("read"),
        websiteUrl = URL(json.getString("website-url"))
    )
}

internal fun DeepLinkPushNotification.Companion.decodeJson(json: JSONObject): RoverPushNotification {
    return DeepLinkPushNotification(
        title = json.getString("android-title"),
        text = json.getString("notification-text"),
        channelId = json.optString("channel-id"),
        contentType = json.getString("content-type"),
        read = json.getBoolean("read"),
        deepLinkUrl = URL(json.getString("deep-link-url"))
    )
}

internal fun ExperiencePushNotification.Companion.decodeJson(json: JSONObject): RoverPushNotification {
    return ExperiencePushNotification(
        title = json.getString("android-title"),
        text = json.getString("notification-text"),
        channelId = json.optString("channel-id"),
        contentType = json.getString("content-type"),
        read = json.getBoolean("read"),
        experienceId = json.getString("experience-id")
    )
}

internal fun RoverPushNotification.Companion.decodeJson(json: JSONObject): RoverPushNotification {
    val contentType = json.getString("content-type")

    return when(contentType) {
        "website" -> WebsitePushNotification.Companion.decodeJson(json)
        "deep-link" -> DeepLinkPushNotification.Companion.decodeJson(json)
        "experience" -> ExperiencePushNotification.Companion.decodeJson(json)
        else -> throw JSONException("Unsupported Rover notification content-type.")
    }
}
