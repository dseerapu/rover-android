package io.rover.location

import com.google.android.gms.location.LocationResult

interface LocationTrackerInterface {
    /**
     * The Google Location Services have yielded a new [LocationResult] to us.
     */
    fun newGoogleLocationResult(locationResult: LocationResult)
}
