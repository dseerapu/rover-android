package io.rover.location

import android.Manifest
import com.google.android.gms.nearby.messages.MessagesClient
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.google.android.gms.nearby.messages.MessagesOptions
import com.google.android.gms.nearby.Nearby
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import io.rover.rover.Rover


/**
 * Subscribes to Beacon updates from Nearby Messages API and emits events and emits beacon
 * reporting events.
 *
 * Google documentation: https://developers.google.com/nearby/messages/android/get-beacon-messages
 */
class GoogleBeaconTrackerService(
    private val applicationContext: Context,
    private val nearbyMessagesClient: MessagesClient,
    private val googleLocationReportingService: GoogleLocationReportingServiceInterface
): GoogleBeaconTrackerServiceInterface {
    override fun newGoogleBeaconMessage(intent: Intent) {
        nearbyMessagesClient.handleIntent(intent, object : MessageListener() {
            override fun onLost(message: Message) {
                googleLocationReportingService.trackExitBeacon(message)
            }

            override fun onFound(message: Message) {
                googleLocationReportingService.trackEnterBeacon(message)
            }
        })
    }

    init {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
