package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.EventSnapshot
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONObject
import java.util.UUID

/**
 * Outgoing JSON DTO transformation for [EventSnapshot]s, as submitted to the Rover GraphQL API.
 * This is equivalent to the `EventInput` structure on the GraphQL API.
 */
internal fun EventSnapshot.asJson(
    dateFormatting: DateFormattingInterface
): JSONObject {
    return JSONObject().apply {
        val props = listOf(
            EventSnapshot::name,
            EventSnapshot::id
        )

        putProp(this@asJson, EventSnapshot::timestamp, { dateFormatting.dateAsIso8601(it, true) })

        props.forEach { putProp(this@asJson, it) }

        putProp(this@asJson, EventSnapshot::attributes) { it.encodeJson() }

        putProp(this@asJson, EventSnapshot::context) { it.asJson() }
    }
}

internal fun EventSnapshot.Companion.decodeJson(json: JSONObject, dateFormatting: DateFormattingInterface): EventSnapshot {
    return EventSnapshot(
        attributes = json.getJSONObject("attributes").toFlatAttributesHash(),
        name = json.getString("name"),
        timestamp = dateFormatting.iso8601AsDate(json.getString("timestamp"), true),
        id = UUID.fromString(json.getString("id")),
        context = Context.decodeJson(json.getJSONObject("context"))
    )
}
