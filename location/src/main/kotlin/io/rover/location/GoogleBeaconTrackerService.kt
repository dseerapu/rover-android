package io.rover.location

import android.Manifest
import android.annotation.SuppressLint
import com.google.android.gms.nearby.messages.MessagesClient
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.google.android.gms.nearby.messages.MessagesOptions
import com.google.android.gms.nearby.Nearby
import android.app.IntentService
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.google.android.gms.nearby.messages.EddystoneUid
import com.google.android.gms.nearby.messages.IBeaconId
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageFilter
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.Strategy
import com.google.android.gms.nearby.messages.SubscribeOptions
import io.rover.location.domain.Region
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.core.permissions.PermissionsNotifierInterface
import io.rover.rover.core.streams.subscribe
import io.rover.rover.notifications.NotificationHandlerInterface
import io.rover.rover.notifications.domain.Notification
import io.rover.rover.platform.whenNotNull
import java.net.URI
import java.util.Date
import java.util.UUID


/**
 * Subscribes to Beacon updates from Nearby Messages API and emits events and emits beacon
 * reporting events.
 *
 * Google documentation: https://developers.google.com/nearby/messages/android/get-beacon-messages
 */
class GoogleBeaconTrackerService(
    private val applicationContext: Context,
    private val nearbyMessagesClient: MessagesClient,
    private val locationReportingService: LocationReportingServiceInterface,
    private val permissionsNotifier: PermissionsNotifierInterface,

    /**
     * Pass in a notification handler to fire notifcations to your Android notification drawer and
     * the app Inbox to debug beacon detection.
     */
    private val debugToNotificationHandler: NotificationHandlerInterface? = null
    ): GoogleBeaconTrackerServiceInterface {
    override fun newGoogleBeaconMessage(intent: Intent) {
        nearbyMessagesClient.handleIntent(intent, object : MessageListener() {
            override fun onFound(message: Message) {
                log.v("A beacon found: $message")
                emitDebugFoundNotification(message)

                messageToBeacon(message).whenNotNull {
                    locationReportingService.trackEnterBeacon(it)
                }
            }

            override fun onLost(message: Message) {
                log.v("A beacon lost: $message")
                emitDebugLostNotification(message)
                messageToBeacon(message).whenNotNull {

                    locationReportingService.trackExitBeacon(it)
                }
            }
        })
    }

    private fun messageToBeacon(message: Message): Region.BeaconRegion? {
        return when(message.type) {
            Message.MESSAGE_TYPE_I_BEACON_ID -> {
                IBeaconId.from(message).toRoverBeaconRegion()
            }
            Message.MESSAGE_TYPE_EDDYSTONE_UID -> {
                val eddystoneUid = EddystoneUid.from(message)
                log.w("Eddystone beacons not currently supported by Rover (uid was $eddystoneUid), and it appears you have one registered with your project. Ignoring.")
                null
            }
            else -> {
                log.w("Unknown beacon type: '${message.type}'. Full payload was '${message.content}'. Ignoring.")
                null
            }
        }
    }

    init {
        // validate that the user specified their Google Nearby API key, which is separate from
        // their google-services.json file, and will silently fail if skipped.
        // validateGoogleNearbyApiKeyPresent()

        startMonitoring()
    }

    @SuppressLint("MissingPermission")
    private fun startMonitoring() {
        permissionsNotifier.notifyForPermission(Manifest.permission.ACCESS_FINE_LOCATION).subscribe {
            log.v("Starting up beacon tracking.")
            val messagesClient = Nearby.getMessagesClient(applicationContext, MessagesOptions.Builder()
                .setPermissions(NearbyPermissions.BLE)
                .build())

            val subscribeOptions = SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .setFilter(
                    MessageFilter.Builder().includeIBeaconIds(UUID.fromString("6A2C6579-1ED8-4307-A6FC-BA9A964EA508"), 0, 0)
                    .build()
                )
                .build()

            messagesClient.subscribe(
                PendingIntent.getService(
                    applicationContext,
                    0,
                    Intent(applicationContext, BeaconReceiverIntentService::class.java),
                    0
                ),
                subscribeOptions
            )
        }
    }

    private fun emitDebugFoundNotification(message: Message) {
//        val notification = NotificationCompat.Builder(applicationContext, NotificationChannel.DEFAULT_CHANNEL_ID)
//            .setContentText("Rover Beacon Debug: Beacon FOUND")
//            .setContentText("Type: ${message.type}")
//            .build()
//
//        NotificationManagerCompat.from(applicationContext).notify(message.hashCode(), notification)

        debugToNotificationHandler?.onMessageReceivedNotification(
            Notification(
                UUID.randomUUID().toString(),
                title = "Rover Beacon Debug: Beacon FOUND at ${Date()}",
                body = "Type: ${message.type}",
                isNotificationCenterEnabled = true,
                uri = URI("https://developers.google.com/nearby/messages/android/get-beacon-messages"),
                isRead = false,
                attachment = null,
                channelId = null,
                deliveredAt = Date(),
                isDeleted = false,
                expiresAt = null
            )
        )
    }

    private fun emitDebugLostNotification(message: Message) {
        debugToNotificationHandler?.onMessageReceivedNotification(
            Notification(
                UUID.randomUUID().toString(),
                title = "Rover Beacon Debug: Beacon LOST at ${Date()}",
                body = "Type: ${message.type}",
                isNotificationCenterEnabled = true,
                uri = URI("https://developers.google.com/nearby/messages/android/get-beacon-messages"),
                isRead = false,
                attachment = null,
                channelId = null,
                deliveredAt = Date(),
                isDeleted = false,
                expiresAt = null
            )
        )
    }

    /**
     * Google Nearby does not seem to validate that its API key is present.
     *
     * We do this here for them to avoid developers being confused by their beacons silently
     * failing to work because they neglected that step.
     */
//    private fun validateGoogleNearbyApiKeyPresent() {
//        val metadata = applicationContext.packageManager.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA).metaData
//
//        metadata.getString("com.google.android.nearby.messages.API_KEY", null)
//            ?: throw RuntimeException("You have Beacon tracking turned on in Rover (setting is passed to LocationAssembler), but you have not yet set up your Google Nearby API key in your manifest.  See documentation.")
//    }
}

class BeaconReceiverIntentService: IntentService("BeaconReceiverIntentService") {
    override fun onHandleIntent(intent: Intent) {
        Handler(Looper.getMainLooper()).post {
            Rover.sharedInstance.resolveSingletonOrFail(GoogleBeaconTrackerServiceInterface::class.java).newGoogleBeaconMessage(
                intent
            )
        }
    }
}

/**
 * Map a received iBeacon back to a Rover.BeaconRegion value object.
 */
fun IBeaconId.toRoverBeaconRegion(): Region.BeaconRegion {
    return Region.BeaconRegion(
        this.proximityUuid,
        this.major.toInt(),
        this.minor.toInt()
    )
}
