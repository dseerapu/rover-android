@file:JvmName("TestHelpers")

package io.rover.rover.platform

import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.domain.EventSnapshot
import io.rover.rover.core.data.domain.Experience
import io.rover.rover.core.data.graphql.WireEncoder
import io.rover.rover.core.data.graphql.operations.data.encodeJson
import org.json.JSONArray
import org.json.JSONObject

// Android's build system (or perhaps the Kotlin compiler) recently changed to apparently not allow
// test code to reference types from the Android SDK (even if it is done so only statically or the
// code itself is classpath shadowed).
//
// Thus we here expose a bit of functionality to allow Kotlin test code to get the useful bits out
// of a few types.

internal fun WireEncoder.encodeEventsToStringJsonForTests(events: List<EventSnapshot>): String =
    this.encodeEventsForSending(events).toString(4)

internal fun WireEncoder.decodeEventSnapshotsFromJsonStringForTests(json: String): List<EventSnapshot> =
    this.decodeEventsForSending(JSONArray(json))

internal fun WireEncoder.decodeDeviceStateFromJsonStringForTests(json: String): DeviceState =
    this.decodeDeviceState(JSONObject(json))

internal fun DeviceState.encodeJsonToStringForTests(dateFormatting: DateFormattingInterface): String = this.encodeJson(dateFormatting).toString(4)

internal fun Experience.encodeJsonToStringForTests(): String = this.encodeJson().toString(4)

internal fun WireEncoder.decodeExperienceFromStringForTests(json: String): Experience =
    this.decodeExperience(JSONObject(json))
