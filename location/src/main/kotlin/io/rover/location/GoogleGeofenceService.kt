package io.rover.location

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import io.rover.location.domain.Region
import io.rover.rover.Rover
import io.rover.rover.core.logging.log

/**
 *
 *
 * Google documentation: https://developer.android.com/training/location/geofencing.html
 */
class GoogleGeofenceService(
    private val applicationContext: Context,
    private val geofencingClient: GeofencingClient,
    private val googleLocationReportingService: GoogleLocationReportingServiceInterface
): GoogleGeofenceServiceInterface {
    override fun newGoogleGeofenceEvent(geofencingEvent: GeofencingEvent) {
        googleLocationReportingService.trackGeofenceEvent(geofencingEvent)
    }

    override fun regionsUpdated(regions: List<Region>) {
        // This will remove all of the Rover geofences, because they are all registered with a
        // pending intent receiver intent service.
        geofencingClient.removeGeofences(
            pendingIntentForReceiverService()
        )

        // now register all of them ourselves:
        geofencingClient.addGeofences(
            // TODO ANDREW START HERE
        )
    }

    /**
     * A Pending Intent for activating the receiver service, [GeofenceReceiverIntentService].
     *
     * The
     */
    private fun pendingIntentForReceiverService(): PendingIntent {
        return PendingIntent.getService(
            applicationContext,
            // I need a geofence-specific requestcode.
            0,
            Intent(applicationContext,  GeofenceReceiverIntentService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

class GeofenceReceiverIntentService: IntentService("GeofenceReceiverIntentService") {
    override fun onHandleIntent(intent: Intent) {
        Handler(Looper.getMainLooper()).post {
            Rover.sharedInstance.resolveSingletonOrFail(GoogleGeofenceServiceInterface::class.java).newGoogleGeofenceEvent(
                GeofencingEvent.fromIntent(intent)
            )
        }
    }
}
