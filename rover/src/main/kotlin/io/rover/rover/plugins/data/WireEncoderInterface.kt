package io.rover.rover.plugins.data

import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Event
import io.rover.rover.plugins.data.domain.Experience
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Wire Encoder is responsible for mapping and transforming the domain model objects
 * into their data-transfer JSON equivalents.
 */
interface WireEncoderInterface {
    fun decodeExperience(data: JSONObject): Experience

    fun decodeDeviceState(data: JSONObject): DeviceState

    fun encodeEventsForSending(events: List<Event>): JSONArray

    fun encodeContextForSending(context: Context): JSONObject

    fun decodeErrors(errors: JSONArray): List<Exception>
}
