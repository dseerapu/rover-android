package io.rover.rover.plugins.data.domain

import io.rover.rover.plugins.events.domain.Event
import java.util.Date
import java.util.UUID

/**
 * An Event as it will be sent to the Rover API.
 *
 * Exactly equivalent to [Event] but also includes the [Context].
 */
data class EventSnapshot(
    val attributes: Attributes,
    val name: String,
    val timestamp: Date,
    val id: UUID,
    val context: Context
) {
    companion object {
        fun fromEvent(event: Event, context: Context): EventSnapshot {
            return EventSnapshot(
                event.attributes,
                event.name,
                event.timestamp,
                event.id,
                context
            )
        }
    }
}
