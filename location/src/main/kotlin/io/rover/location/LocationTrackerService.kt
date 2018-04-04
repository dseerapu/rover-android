package io.rover.location

import android.Manifest
import android.app.IntentService
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_NO_POWER
import com.google.android.gms.location.LocationResult
import io.rover.rover.Rover
import io.rover.rover.core.events.EventQueueServiceInterface
import io.rover.rover.core.logging.log

/**
 * Subscribes to Location Updates from FusedLocationManager.
 *
 *
 */
class LocationTrackerService(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val applicationContext: Context,
    private val eventQueueService: EventQueueServiceInterface
): LocationTrackerInterface {
    override fun newGoogleLocationResult(locationResult: LocationResult) {
        log.v("Received location result: $locationResult")
    }

    init {
        if(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient
                .requestLocationUpdates(
                    LocationRequest
                        .create()
                        .setInterval(1)
                        .setFastestInterval(1)
                        .setSmallestDisplacement(0f)
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY),
                    PendingIntent.getService(
                        applicationContext,
                        0,
                        Intent(applicationContext, LocationReceiverIntentService::class.java),
                        0
                    )
                )
        }

        // use the pending intent version of requestLocationUpdates so background works!

        // although, is the pendingintent version kosher for O?

        // also perhaps this can be coarse? only used by backend to identify what geofences should
        // be offered to us. hm, looks like doc says we must use Fine anyway.

    }
}

class LocationReceiverIntentService: IntentService("LocationReceiverIntentService") {
    override fun onHandleIntent(intent: Intent) {
        Handler(Looper.getMainLooper()).post {
            if (LocationResult.hasResult(intent)) {
                val result = LocationResult.extractResult(intent)

                Rover.sharedInstance.resolveSingletonOrFail(LocationTrackerInterface::class.java).newGoogleLocationResult(result)
            } else {
                log.w("LocationReceiver received an intent, but it lacked a location result. Ignoring.")
            }
        }
    }
}