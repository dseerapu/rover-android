package io.rover.rover.plugins.data.requests.data

import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONObject

/**
 * Outgoing JSON DTO transformation for [Event]s, as submitted to the Rover GraphQL API.  This is
 * equivalent to the `EventInput` structure on the GraphQL API.
 */
fun Event.asJson(
    dateFormatting: DateFormattingInterface
): JSONObject {
    return JSONObject().apply {
        val props = listOf(
            Event::name,
            Event::id
        )

        putProp(this@asJson, Event::timestamp, { dateFormatting.dateAsIso8601(it) })

        props.forEach { putProp(this@asJson, it) }

        putProp(this@asJson, Event::attributes) { it.encodeJson() }
    }
}