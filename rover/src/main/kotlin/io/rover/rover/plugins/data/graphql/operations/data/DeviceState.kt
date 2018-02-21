package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.domain.DeviceState
import io.rover.rover.plugins.data.domain.Profile
import io.rover.rover.plugins.data.domain.PushNotification
import io.rover.rover.plugins.data.domain.Region
import io.rover.rover.plugins.data.graphql.getObjectIterable
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONArray
import org.json.JSONObject


internal fun DeviceState.Companion.decodeJson(jsonObject: JSONObject, dateFormatting: DateFormattingInterface): DeviceState {
    return DeviceState(
        profile = Profile.decodeJson(jsonObject.getJSONObject("profile")),
        regions = jsonObject.getJSONArray("regions").getObjectIterable().map { regionsJson ->
            Region.decodeJson(regionsJson)
        }.toSet(),
        notifications = jsonObject.getJSONArray("notifications").getObjectIterable().map { notificationsJson ->
            PushNotification.decodeJson(notificationsJson, dateFormatting)
        }
    )
}

internal fun DeviceState.encodeJson(dateFormatting: DateFormattingInterface): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, DeviceState::profile) { it.encodeJson() }
        putProp(this@encodeJson, DeviceState::regions) { JSONArray(it.map { it.encodeJson() }) }
        putProp(this@encodeJson, DeviceState::notifications) { JSONArray(it.map { it.encodeJson(dateFormatting) }) }
    }
}
