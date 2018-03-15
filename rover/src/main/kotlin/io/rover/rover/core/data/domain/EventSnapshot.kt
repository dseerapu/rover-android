package io.rover.rover.core.data.domain

import io.rover.rover.core.events.domain.Event
import java.util.Date
import java.util.UUID

/**
 * An Event as it will be sent to the Rover API.
 *
 * Exactly equivalent to [Event] but also includes the [Context].
 */
data class EventSnapshot(
    val name: String,
    val attributes: Attributes,
    val timestamp: Date,
    val id: UUID,
    val namespace: String,
    val context: Context
) {
    companion object {
        fun fromEvent(event: Event, context: Context): EventSnapshot {
            return EventSnapshot(
                event.name,
                event.attributes,
                event.timestamp,
                event.id,
                event.namespace,
                context
            )
        }
    }
}
