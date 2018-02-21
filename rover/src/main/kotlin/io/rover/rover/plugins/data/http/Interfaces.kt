package io.rover.rover.plugins.data.http

import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.EventSnapshot
import io.rover.rover.plugins.data.domain.Experience
import io.rover.rover.plugins.data.domain.Notification
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Wire Encoder is responsible for mapping and transforming the domain model objects
 * into their data-transfer JSON equivalents.
 */
interface WireEncoderInterface {
    fun decodeExperience(data: JSONObject): Experience

    fun decodeDeviceState(data: JSONObject): DeviceState

    fun decodeNotification(data: JSONObject): Notification

    fun encodeEventsForSending(events: List<EventSnapshot>): JSONArray

    fun encodeContextForSending(context: Context): JSONObject

    fun decodeErrors(errors: JSONArray): List<Exception>
}

interface NetworkClient {
    /**
     * Perform the given HttpRequest and then deliver the result to the given [completionHandler].
     *
     * Note that [completionHandler] is given an [HttpClientResponse], which includes readable
     * streams.  Thus, it is called on the background worker thread to allow for client code to
     * read those streams, safely away from the Android main UI thread.
     */
    fun networkTask(
        request: HttpRequest,
        bodyData: String?,
        completionHandler: (HttpClientResponse) -> Unit
    ): NetworkTask
}

/**
 * A cancellable concurrent operation.
 */
interface NetworkTask {
    fun cancel()
    fun resume()
}