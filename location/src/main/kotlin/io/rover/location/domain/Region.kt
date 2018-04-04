package io.rover.location.domain

import java.util.UUID

sealed class Region {
    data class BeaconRegion(
        val uuid: UUID,
        val major: Int?,
        val minor: Int?
    ) : Region() {
        companion object
    }

    data class GeofenceRegion(
        val latitude: Double,
        val longitude: Double,
        val radius: Double
    ) : Region() {
        companion object
    }

    companion object
}
