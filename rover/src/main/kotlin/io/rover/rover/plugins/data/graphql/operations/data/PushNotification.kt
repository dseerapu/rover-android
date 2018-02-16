package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.data.domain.PushNotification
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URL

internal fun PushNotification.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, PushNotification::id, "id")
        putProp(this@encodeJson, PushNotification::title, "android-title")
        putProp(this@encodeJson, PushNotification::text, "notification-text")
        putProp(this@encodeJson, PushNotification::channelId, "channel-id")
        putProp(this@encodeJson, PushNotification::contentType, "content-type")
        putProp(this@encodeJson, PushNotification::isNotificationCenterEnabled, "is-notification-center-enabled")
        putProp(this@encodeJson, PushNotification::read, "read")
        putProp(this@encodeJson, PushNotification::action, "action" ) {
            it.encodeJson()
        }
    }
}

internal fun PushNotificationAction.Companion.decodeJson(contentType: String, json: JSONObject): PushNotificationAction {
    return when(contentType) {
        "website" -> PushNotificationAction.Website(
            websiteUrl = URL(json.getString("website-url"))
        )
        "deep-link" -> PushNotificationAction.DeepLink(
            deepLinkUrl = URI(json.getString("deep-link-url"))
        )
        "experience" -> PushNotificationAction.Experience(
            experienceId = json.getString("experience-id")
        )
        else -> throw JSONException("Unsupported Rover notification content-type.")
    }
}

internal fun PushNotificationAction.encodeJson(): JSONObject {
    return JSONObject().apply {
        when(this@encodeJson) {
            is PushNotificationAction.Experience -> {
                putProp(this@encodeJson, PushNotificationAction.Experience::experienceId, "experience-id")
            }
            is PushNotificationAction.DeepLink -> {
                putProp(this@encodeJson, PushNotificationAction.DeepLink::deepLinkUrl, "deep-link-url")
            }
            is PushNotificationAction.Website -> {
                putProp(this@encodeJson, PushNotificationAction.Website::websiteUrl, "website-url")
            }
        }
    }
}

internal fun PushNotification.Companion.decodeJson(json: JSONObject): PushNotification {
    return PushNotification(
        id = json.getString("id"),
        title = json.getString("android-title"),
        text = json.getString("notification-text"),
        channelId = json.optString("channel-id", null),
        contentType = json.getString("content-type"),
        read = json.getBoolean("read"),
        isNotificationCenterEnabled = json.getBoolean("is-notification-center-enabled"),
        action = PushNotificationAction.decodeJson(json.getString("content-type"), json.getJSONObject("action"))
    )
}
