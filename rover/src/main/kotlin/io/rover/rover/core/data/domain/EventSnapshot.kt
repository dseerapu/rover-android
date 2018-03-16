package io.rover.rover.core.data.domain

import io.rover.rover.core.events.domain.Event
import java.util.Date
import java.util.UUID

/**
 * An Event as it will be sent to the Rover API.
 *
 * Exactly equivalent to [Event] but also includes some metadata, namely the [Context] and the
 * namespace.
 */
data class EventSnapshot(
    val name: String,
    val attributes: Attributes,
    val timestamp: Date,
    val id: UUID,
    val namespace: String?,
    val context: Context
) {
    companion object {
        /**
         * Add a few elements of metadata to an [Event] to create an [EventSnapshot]
         *
         * @param namespace Specify a namespace name to your events in order to separate them from
         * other events.  They will appear as a separate table in your BigQuery instance. May be
         * left null to have it appear in a default table.
         */
        fun fromEvent(event: Event, context: Context, namespace: String?): EventSnapshot {
            return EventSnapshot(
                event.name,
                event.attributes,
                event.timestamp,
                event.id,
                namespace,
                context
            )
        }
    }
}
