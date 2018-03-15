package io.rover.rover.core.events.domain

import io.rover.rover.core.data.domain.Attributes
import java.util.Date
import java.util.UUID

data class Event(
    val name: String,
    val attributes: Attributes,
    val timestamp: Date,
    val id: UUID,
    val namespace: String
) {
    constructor(
        name: String,
        attributes: Attributes
    ): this(name, attributes, Date(), UUID.randomUUID(), ROVER_NAMESPACE)
    companion object {
        const val ROVER_NAMESPACE = "rover"
    }
}
