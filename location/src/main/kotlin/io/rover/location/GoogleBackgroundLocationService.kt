package io.rover.location

import android.Manifest
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import io.rover.rover.Rover
import io.rover.rover.core.logging.log

/**
 * Subscribes to Location Updates from FusedLocationManager and emits location reporting events.
 *
 * Google documentation: https://developer.android.com/training/location/receive-location-updates.html
 */
class GoogleBackgroundLocationService(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val applicationContext: Context,
    private val googleLocationReportingService: GoogleLocationReportingServiceInterface
): GoogleBackgroundLocationServiceInterface {
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
    }
}

class LocationReceiverIntentService: IntentService("LocationReceiverIntentService") {
    override fun onHandleIntent(intent: Intent) {
        Handler(Looper.getMainLooper()).post {
            if (LocationResult.hasResult(intent)) {
                val result = LocationResult.extractResult(intent)

                Rover.sharedInstance.resolveSingletonOrFail(GoogleBackgroundLocationServiceInterface::class.java).newGoogleLocationResult(result)
            } else {
                log.w("LocationReceiver received an intent, but it lacked a location result. Ignoring.")
            }
        }
    }
}