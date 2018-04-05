package io.rover.location.domain

import java.util.UUID

sealed class Region {
    /**
     * Note: Beacons are not individually monitored on Android with Google Nearby, so this is
     * unused.
     */
    data class BeaconRegion(
        val uuid: UUID,
        val major: Int?,
        val minor: Int?
    ) : Region() {
        override val identifier: String
            get() = when {
                major != null && minor != null -> "$uuid:$major:$minor"
                major != null -> "$uuid:$major"
                else -> uuid.toString()
            }

        companion object
    }

    data class GeofenceRegion(
        val latitude: Double,
        val longitude: Double,
        val radius: Double
    ) : Region() {
        override val identifier: String
            get() = "$latitude:$longitude:$radius"

        companion object
    }

    abstract val identifier: String

    companion object
}
