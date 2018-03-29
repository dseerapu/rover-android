package io.rover.rover.core.data.domain

import io.rover.rover.notifications.domain.Notification

data class DeviceState(
    val profile: Profile,
    val regions: Set<Region>,
    val notifications: List<Notification>
) {
    companion object
}
