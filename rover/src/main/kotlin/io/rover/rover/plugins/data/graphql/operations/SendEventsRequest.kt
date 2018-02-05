package io.rover.rover.plugins.data.graphql.operations

import io.rover.rover.plugins.data.NetworkRequest
import io.rover.rover.plugins.data.domain.EventSnapshot
import io.rover.rover.plugins.data.http.WireEncoderInterface
import org.json.JSONObject

class SendEventsRequest(
    events: List<EventSnapshot>,
    wireEncoder: WireEncoderInterface
) : NetworkRequest<String> {
    override val operationName: String = "TrackEvents"

    override val query: String = """
        mutation TrackEvents(${"\$"}events: [Event]!) {
            trackEvents(events:${"\$"}events)
        }
    """

    override val variables: JSONObject = JSONObject().apply {
        put("events", wireEncoder.encodeEventsForSending(events))
    }

    override fun decodePayload(responseObject: JSONObject, wireEncoder: WireEncoderInterface): String =
        responseObject.getString("data")
}
