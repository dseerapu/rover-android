package io.rover.rover.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.DrawableRes
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import io.rover.rover.core.logging.log
import io.rover.rover.core.streams.Observable
import io.rover.rover.core.streams.onErrorReturn
import io.rover.rover.core.streams.subscribe
import io.rover.rover.core.streams.timeout
import io.rover.rover.core.data.NetworkResult
import io.rover.rover.notifications.domain.NotificationAttachment
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.data.http.WireEncoderInterface
import io.rover.rover.core.assets.AssetService
import io.rover.rover.notifications.ui.NotificationsRepositoryInterface
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.util.concurrent.TimeUnit
import android.content.Context.NOTIFICATION_SERVICE
import android.support.annotation.RequiresApi
import io.rover.rover.R


open class NotificationHandler(
    private val applicationContext: Context,

    // TODO change to private val pushTokenTransmissionChannel: PushTokenTransmissionChannel,
    private val eventsPlugin: EventQueueServiceInterface,

    private val wireEncoder: WireEncoderInterface,

    // add this back after solving injection issues.
    private val notificationsRepository: NotificationsRepositoryInterface,

    private val notificationOpen: NotificationOpenInterface,

    private val assetService: AssetService,

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
): NotificationHandlerInterface {

    override fun onTokenRefresh(token: String?) {
        // so, we need the token to be consumable from a FirebasePushTokenContextProvider

        // TODO to make things safer for GCM consumers, which may be calling this off the main
        // thread, manually delegate this to the main thread just in case.
        Handler(Looper.getMainLooper()).post {
            eventsPlugin.setPushToken(token)
        }
    }

    /**
     * Process an the parameters from an incoming notification.
     *
     * Note that this is running in the context of a 10 second wallclock execution time restriction.
     */
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

    override fun onMessageReceivedNotification(notification: io.rover.rover.notifications.domain.Notification) {
        verifyChannelSetUp()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, notification.channelId ?: defaultChannelId ?: NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(applicationContext)
        }

        builder.setContentTitle(notification.title)
        builder.setContentText(notification.body)
        builder.setSmallIcon(smallIconResId, smallIconDrawableLevel)

        // Add this back after solving injection issues.
        notificationsRepository.notificationArrivedByPush(notification)

        builder.setContentIntent(
            notificationOpen.pendingIntentForAndroidNotification(
                notification
            )
        )

        // Set large icon and Big Picture as needed by Rich Media values.  Enforce a timeout
        // so we don't fail to create the notification in the allotted 10s if network doesn't
        // cooperate.
        val attachmentBitmapPublisher = when(notification.attachment) {
            is NotificationAttachment.Image -> {
                assetService.getImageByUrl(notification.attachment.url)
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .onErrorReturn { error ->
                        // log.w("Timed out fetching notification image.  Will create image without the rich media.")
                        NetworkResult.Error(error, false)
                    }
            }
            null -> Observable.just(null)
            else -> {
                log.w("Notification attachments of type ${notification.attachment.typeName} not supported on Android.")
                Observable.just(null)
            }
        }

        attachmentBitmapPublisher
            .subscribe { attachmentBitmapResult ->
                when(attachmentBitmapResult) {
                    is NetworkResult.Success -> {
                        builder.setLargeIcon(attachmentBitmapResult.response)
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(attachmentBitmapResult.response)
                        )
                    }
                    is NetworkResult.Error -> {
                        log.w("Unable to retrieve notification image: ${notification.attachment?.url}, because: ${attachmentBitmapResult.throwable.message}")
                        log.w("Will create image without the rich media.")
                    }
                }
                notificationManager.notify(notification.id, 123, builder.build().apply { this.flags = this.flags or Notification.FLAG_AUTO_CANCEL })
            }
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

        onMessageReceivedNotification(pushNotification)
    }

    /**
     * By default, if running on Oreo and later, and the [NotificationHandler.defaultChannelId] you
     * gave does not exist, then we will lazily create it at notification reception time to
     * avoid the
     *
     * We include a default implementation here,
     */
    @RequiresApi(Build.VERSION_CODES.O)
    open fun registerDefaultChannelId() {
        log.w("Rover is registering a default channel ID for you.  This isn't optimal; if you are targeting Android SDK >= 26 then you should create your Notification Channels.\n" +
            "See https://developer.android.com/training/notify-user/channels.html")
        // Create the NotificationChannel
        val name = applicationContext.getString(R.string.default_notification_channel_name)
        val description = applicationContext.getString(R.string.default_notification_description)

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(defaultChannelId, name, importance)
        mChannel.description = description
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    @SuppressLint("NewApi")
    private fun verifyChannelSetUp() {
        val notificationManager = applicationContext.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager

        val existingChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(defaultChannelId)
        } else {
            return
        }

        if(existingChannel == null) registerDefaultChannelId()
    }

    companion object {
        /**
         * Android gives push handlers 10 seconds to complete.
         *
         * If we can't get our image downloaded in the 10 seconds, instead of failing we want to
         * timeout gracefully.
         */
        private const val TIMEOUT_SECONDS = 8L
    }
}
