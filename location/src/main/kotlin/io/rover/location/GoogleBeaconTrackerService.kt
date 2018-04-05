package io.rover.location

import android.Manifest
import android.annotation.SuppressLint
import com.google.android.gms.nearby.messages.MessagesClient
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.google.android.gms.nearby.messages.MessagesOptions
import com.google.android.gms.nearby.Nearby
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.android.gms.nearby.messages.EddystoneUid
import com.google.android.gms.nearby.messages.IBeaconId
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import io.rover.location.domain.Region
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.core.permissions.PermissionsNotifierInterface
import io.rover.rover.core.streams.subscribe
import io.rover.rover.platform.whenNotNull


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
    private val permissionsNotifier: PermissionsNotifierInterface
    ): GoogleBeaconTrackerServiceInterface {
    override fun newGoogleBeaconMessage(intent: Intent) {
        nearbyMessagesClient.handleIntent(intent, object : MessageListener() {
            override fun onFound(message: Message) {
                messageToBeacon(message).whenNotNull { locationReportingService.trackEnterBeacon(
                    it
                )}
            }

            override fun onLost(message: Message) {
                messageToBeacon(message).whenNotNull { locationReportingService.trackExitBeacon(
                    it
                )}
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
                log.w("Eddystone beacons not currently supported by Rover (uid was $eddystoneUid). Ignoring")
                null
            }
            else -> {
                log.w("Unknown beacon type: '${message.type}', ignoring.")
                null
            }
        }
    }

    init {
        startMonitoring()
    }

    @SuppressLint("MissingPermission")
    private fun startMonitoring() {
        permissionsNotifier.notifyForPermission(Manifest.permission.ACCESS_FINE_LOCATION).subscribe {
            val messagesClient = Nearby.getMessagesClient(applicationContext, MessagesOptions.Builder()
                .setPermissions(NearbyPermissions.BLE)
                .build())

            messagesClient.subscribe(
                PendingIntent.getService(
                    applicationContext,
                    0,
                    Intent(applicationContext, BeaconReceiverIntentService::class.java),
                    0
                )
            )
        }
    }
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
