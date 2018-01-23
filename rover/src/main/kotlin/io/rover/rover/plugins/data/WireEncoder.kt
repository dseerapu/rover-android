package io.rover.rover.plugins.data

import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.requests.data.asJson
import io.rover.rover.plugins.data.requests.data.decodeJson
import io.rover.rover.plugins.data.requests.data.getObjectIterable
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

    fun decodeContext(data: String): Context {
        val json = JSONObject(data)
        return Context.Companion.decodeJson(json)
    }

    /**
     * Encode a list of events for submission to the cloud-side API.
     */
    override fun encodeEventsForSending(events: List<Event>): JSONArray =
        JSONArray(
            events.map { it.asJson(dateFormatting) }
        )

    override fun decodeExperience(data: JSONObject): Experience = Experience.decodeJson(data)

    override fun decodeDeviceState(data: JSONObject): DeviceState = DeviceState.decodeJson(data)

    override fun decodeErrors(errors: JSONArray): List<Exception> {
        return errors.getObjectIterable().map {
            // TODO: change to a better type than just Exception.  perhaps one with best-effort decoding of the GraphQL errors object.
            Exception(it.toString())
        }
    }
}
