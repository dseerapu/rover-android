package io.rover.location

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.location.GeofencingRequest
import io.rover.location.domain.Region
import io.rover.rover.Rover
import io.rover.rover.core.logging.log
import io.rover.rover.core.permissions.PermissionsNotifierInterface
import io.rover.rover.core.streams.subscribe

/**
 * Monitors for Geofence events using the Google Location Geofence API.
 *
 * Monitors the list of appropriate geofences to subscribe to as defined by the Rover API
 * via the [RegionObserver] interface.
 *
 * Google documentation: https://developer.android.com/training/location/geofencing.html
 */
class GoogleGeofenceService(
    private val applicationContext: Context,
    private val geofencingClient: GeofencingClient,
    private val locationReportingService: LocationReportingServiceInterface,
    private val permissionsNotifier: PermissionsNotifierInterface
    // TODO: customizable geofence limit
): GoogleGeofenceServiceInterface {
    override fun newGoogleGeofenceEvent(geofencingEvent: GeofencingEvent) {

        // have to do processing here because we need to know what the regions are.

        if(geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(
                geofencingEvent.errorCode
            )

            log.w("Unable to capture Geofence message because: $errorMessage")

            val regions = geofencingEvent.triggeringGeofences.map {
                val fence = geofencingEvent.triggeringGeofences.first()

                val region = currentFences.firstOrNull { it.identifier == fence.requestId }

                if(region == null) {
                    val verb = when(geofencingEvent.geofenceTransition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
                        Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
                        else -> "unknown (${geofencingEvent.geofenceTransition})"
                    }
                    log.w("Received an $verb event for Geofence with request-id/identifier '${fence.requestId}', but not currently tracking that one. Ignoring.")
                }
                region
            }.filterNotNull()

            regions.forEach { region ->
                when (geofencingEvent.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> locationReportingService.trackEnterGeofence(
                        region
                    )
                    Geofence.GEOFENCE_TRANSITION_EXIT -> locationReportingService.trackExitGeofence(
                        region
                    )
                }
            }
        }
    }

    override fun regionsUpdated(regions: List<Region>) {
        currentFences = regions.filterIsInstance(Region.GeofenceRegion::class.java)

        updateGeofencesIfPossible()
    }

    @SuppressLint("MissingPermission")
    private fun updateGeofencesIfPossible() {
        if(permissionObtained && currentFences.isNotEmpty()) {
            log.v("Updating geofences.")
            // TODO: andrew start here and figure out how to get something to happen when both regions
            // and perms are ready at least once.  Create combineLatest operator.

            // However, I need this to be readily reimplementable by developers.  I would

            // This will remove any existing Rover geofences, because will all be registered with the
            // same pending intent pointing to the receiver intent service.
            geofencingClient.removeGeofences(
                pendingIntentForReceiverService()
            )

            val geofences = currentFences.map { region ->
                Geofence.Builder()
                    .setRequestId(region.identifier)
                    .setCircularRegion(
                        region.latitude,
                        region.longitude,
                        region.radius.toFloat()
                    )
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .addGeofences(geofences)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build()

            // TODO: shoot this cannot be done until the permissions are asked for.
            // now register all of them ourselves:
            geofencingClient.addGeofences(request, pendingIntentForReceiverService())
            log.v("Now monitoring ${geofences.count()} Rover geofences.")
        }
    }

    /**
     * A Pending Intent for activating the receiver service, [GeofenceReceiverIntentService].
     *
     * The
     */
    private fun pendingIntentForReceiverService(): PendingIntent {
        return PendingIntent.getService(
            applicationContext,
            0,
            Intent(applicationContext,  GeofenceReceiverIntentService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private var currentFences: List<Region.GeofenceRegion> = listOf()

    private var permissionObtained = false

    init {
        permissionsNotifier.notifyForPermission(Manifest.permission.ACCESS_FINE_LOCATION).subscribe {
            permissionObtained = true
            updateGeofencesIfPossible()
        }
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
