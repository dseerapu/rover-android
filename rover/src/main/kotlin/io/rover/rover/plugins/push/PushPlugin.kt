package io.rover.rover.plugins.push

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import io.rover.rover.core.logging.log
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.userexperience.NotificationOpenInterface
import io.rover.rover.plugins.userexperience.notificationcentre.NotificationsRepositoryInterface
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException

open class PushPlugin(
    private val applicationContext: Context,

    // TODO change to private val pushTokenTransmissionChannel: PushTokenTransmissionChannel,
    private val eventsPlugin: EventsPluginInterface,

    private val wireEncoder: WireEncoderInterface,

    // add this back after solving injection issues.
    // private val notificationsRepository: NotificationsRepositoryInterface,

    private val notificationOpen: NotificationOpenInterface,

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

        // TODO to make things safer for GCM consumers, which may be calling this off the main
        // thread, manually delegate this to the main thread just in case.
        Handler(Looper.getMainLooper()).post {
            eventsPlugin.setPushToken(token)
        }
    }

    override fun onMessageReceivedData(parameters: Map<String, String>) {
        // if we have been called, then:
        // a) the notification does not have a display message component; OR
        // b) the app is running in foreground.

        log.v("Received a push notification. Raw parameters: $parameters")

        if(!parameters.containsKey("rover")) {
            log.w("Invalid push notification received: `rover` data parameter not present. Possibly was a Display-only push notification, or otherwise not intended for the Rover SDK. Ignoring.")
        }

        val rover = parameters["rover"] ?: return
        handleRoverNotificationObject(rover)
    }

    override fun onMessageReceivedDataAsBundle(parameters: Bundle) {
        val rover = parameters.getString("rover") ?: return
        handleRoverNotificationObject(rover)
    }

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)

    private fun handleRoverNotificationObject(roverJson: String) {
        val pushNotification = try {
            val roverJsonObject = JSONObject(roverJson)
            val notificationJson = roverJsonObject.getJSONObject("notification")
            wireEncoder.decodeNotification(notificationJson)
        } catch (e: JSONException) {
            log.w("Invalid push notification received: `$roverJson`, resulting in '${e.message}'. Ignoring.")
            return
        } catch (e: MalformedURLException) {
            log.w("Invalid push notification received: `$roverJson`, resulting in '${e.message}'. Ignoring.")
            return
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, pushNotification.channelId ?: defaultChannelId ?: NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(applicationContext)
        }

        builder.setContentTitle(pushNotification.title)
        builder.setContentText(pushNotification.body)
        builder.setSmallIcon(smallIconResId, smallIconDrawableLevel)

        // Add this back after solving injection issues.
        // notificationsRepository.notifcationArrivedByPush(pushNotification)

        builder.setContentIntent(
            notificationOpen.pendingIntentForAndroidNotification(
                pushNotification
            )
        )

        // TODO: set large icon and possibly a big picture style as needed by Rich Media values. Protocol to be determined.


        // lol start here
        notificationManager.notify(pushNotification.id, 123, builder.build().apply { this.flags = this.flags or Notification.FLAG_AUTO_CANCEL })
    }
}
