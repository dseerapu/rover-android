package io.rover.rover.core.data.graphql.operations.data

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.core.data.domain.DeviceState
import io.rover.rover.core.data.domain.Profile
import io.rover.rover.notifications.domain.Notification
import io.rover.rover.core.data.domain.Region
import io.rover.rover.core.data.graphql.getObjectIterable
import io.rover.rover.core.data.graphql.putProp
import io.rover.rover.notifications.graphql.decodeJson
import io.rover.rover.notifications.graphql.encodeJson
import org.json.JSONArray
import org.json.JSONObject


internal fun DeviceState.Companion.decodeJson(jsonObject: JSONObject, dateFormatting: DateFormattingInterface): DeviceState {
    return DeviceState(
        profile = Profile.decodeJson(jsonObject.getJSONObject("profile")),
        regions = jsonObject.getJSONArray("regions").getObjectIterable().map { regionsJson ->
            Region.decodeJson(regionsJson)
        }.toSet(),
        notifications = jsonObject.getJSONArray("notifications").getObjectIterable().map { notificationsJson ->
            Notification.decodeJson(notificationsJson, dateFormatting)
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
