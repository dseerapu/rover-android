package io.rover.location

import android.content.Intent
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationResult
import com.google.android.gms.nearby.messages.Message
import io.rover.location.domain.Region

interface GoogleBackgroundLocationServiceInterface {
    /**
     * The Google Location Services have yielded a new [LocationResult] to us.
     */
    fun newGoogleLocationResult(locationResult: LocationResult)
}

interface GoogleBeaconTrackerServiceInterface {
    fun newGoogleBeaconMessage(intent: Intent)
}

interface GoogleGeofenceServiceInterface: RegionObserver {
    fun newGoogleGeofenceEvent(geofencingEvent: GeofencingEvent)
}

interface RegionRepositoryInterface {
    /**
     * Register an object that should be updated whenever the regions are updated.
     */
    fun registerObserver(regionObserver: RegionObserver)
}

interface RegionObserver {
    /**
     * Called when the Rover regions change.
     */
    fun regionsUpdated(regions: List<Region>)
}

/**
 * Dispatch location events to Rover that have been created by the various relevant Google Play
 * services.
 *
 * TODO will likely create a LocationReportingService that might accept abstract non-Google data
 * types instead, and then this class will emit events to that.
 */
interface GoogleLocationReportingServiceInterface {
    fun trackGeofenceEvent(geofencingEvent: GeofencingEvent)

    fun trackEnterBeacon(message: Message)

    fun trackExitBeacon(message: Message)

    fun updateLocation(
        location: LocationResult
    )
}
