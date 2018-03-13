package io.rover.rover.core.data.domain

data class DeviceState(
    val profile: Profile,
    val regions: Set<Region>,
    val notifications: List<Notification>
) {
    companion object
}
