package io.rover.rover.plugins.data.domain

import java.util.Date
import java.util.UUID

data class Event(
    val attributes: Attributes,
    val name: String,
    val timestamp: Date,
    val id: UUID
) {
    companion object
}
