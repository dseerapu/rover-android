package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Profile
import io.rover.rover.plugins.data.domain.Region
import io.rover.rover.plugins.data.graphql.getObjectIterable
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONArray
import org.json.JSONObject

fun DeviceState.Companion.decodeJson(jsonObject: JSONObject): DeviceState {
    return DeviceState(
        profile = Profile.decodeJson(jsonObject.getJSONObject("profile")),
        regions = jsonObject.getJSONArray("regions").getObjectIterable().map {
            Region.decodeJson(it)
        }.toSet()
    )
}

fun DeviceState.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, DeviceState::profile) { it.encodeJson() }
        putProp(this@encodeJson, DeviceState::regions) { JSONArray(it.map { it.encodeJson() }) }
    }
}
