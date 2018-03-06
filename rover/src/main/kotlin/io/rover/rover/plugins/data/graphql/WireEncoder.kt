package io.rover.rover.plugins.data.graphql

import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.domain.EventSnapshot
import io.rover.rover.plugins.data.domain.Notification
import io.rover.rover.plugins.data.http.WireEncoderInterface
import io.rover.rover.plugins.data.graphql.operations.data.asJson
import io.rover.rover.plugins.data.graphql.operations.data.decodeJson
import io.rover.rover.plugins.data.graphql.operations.data.encodeJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Responsible for marshalling Data Transfer objects to and from
 * their appropriate wire-format representation expected by the Rover API.
 */
class WireEncoder(
    private val dateFormatting: DateFormattingInterface
) : WireEncoderInterface {
    override fun encodeContextForSending(context: Context): JSONObject {
        return context
            .asJson()
    }

    override fun encodeNotification(notification: Notification): JSONObject {
        return notification.encodeJson(dateFormatting)
    }

    override fun decodeEventsForSending(data: JSONArray): List<EventSnapshot> {
        return data.getObjectIterable().map {
            EventSnapshot.decodeJson(it, dateFormatting)
        }
    }

    fun decodeContext(data: String): Context {
        val json = JSONObject(data)
        return Context.Companion.decodeJson(json)
    }

    override fun decodeNotification(data: JSONObject): Notification {
        return Notification.Companion.decodeJson(data, dateFormatting)
    }

    /**
     * Encode a list of events for submission to the cloud-side API.
     */
    override fun encodeEventsForSending(events: List<EventSnapshot>): JSONArray =
        JSONArray(
            events.map { it.asJson(dateFormatting) }
        )

    override fun decodeExperience(data: JSONObject): Experience = Experience.decodeJson(data)

    override fun decodeDeviceState(data: JSONObject): DeviceState = DeviceState.decodeJson(data, dateFormatting)

    override fun decodeErrors(errors: JSONArray): List<Exception> {
        return errors.getObjectIterable().map {
            // TODO: change to a better type than just Exception.  perhaps one with best-effort decoding of the GraphQL errors object.
            Exception(it.toString())
        }
    }
}
