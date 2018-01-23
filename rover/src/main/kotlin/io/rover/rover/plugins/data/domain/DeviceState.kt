package io.rover.rover.plugins.data.domain

data class DeviceState(
    val profile: Profile,
    val regions: Set<Region>
) {
    companion object
}
