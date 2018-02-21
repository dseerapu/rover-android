package io.rover.rover.plugins.data.graphql.operations.data

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.plugins.data.domain.PushNotificationAction
import io.rover.rover.plugins.data.domain.PushNotification
import io.rover.rover.plugins.data.graphql.putProp
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URL

internal fun PushNotification.encodeJson(dateFormatting: DateFormattingInterface): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, PushNotification::id, "id")
        putProp(this@encodeJson, PushNotification::title, "title")
        putProp(this@encodeJson, PushNotification::body, "body")
        putProp(this@encodeJson, PushNotification::channelId, "channelId")
        putProp(this@encodeJson, PushNotification::isNotificationCenterEnabled, "isNotificationCenterEnabled")
        putProp(this@encodeJson, PushNotification::read, "isRead")
        putProp(this@encodeJson, PushNotification::deleted, "isDeleted")
        putProp(this@encodeJson, PushNotification::deliveredAt, "deliveredAt") { dateFormatting.dateAsIso8601(it )}
        putProp(this@encodeJson, PushNotification::expiresAt, "expiresAt") { dateFormatting.dateAsIso8601(it )}
        putProp(this@encodeJson, PushNotification::action, "action" ) {
            it.encodeJson()
        }
    }
}

internal fun PushNotificationAction.Companion.decodeJson(json: JSONObject): PushNotificationAction {
    val typeName = json.getString("__typename")
    return when(typeName) {
        "PresentWebsiteNotificationAction" -> PushNotificationAction.PresentWebsite(
            url = URL(json.getString("url"))
        )
        "OpenURLNotificationAction" -> PushNotificationAction.OpenUrl(
            url = URI(json.getString("url"))
        )
        "PresentExperienceNotificationAction" -> PushNotificationAction.PresentExperience(
            experienceId = json.getString("experienceId")
        )
        else -> throw JSONException("Unsupported Rover notification type: $typeName.")
    }
}

internal fun PushNotificationAction.encodeJson(): JSONObject {
    return JSONObject().apply {
        put(
            "__typename",
            when(this@encodeJson) {
                is PushNotificationAction.PresentExperience -> {
                    putProp(this@encodeJson, PushNotificationAction.PresentExperience::experienceId, "experienceId")
                    "PresentExperienceNotificationAction"
                }
                is PushNotificationAction.OpenUrl -> {
                    putProp(this@encodeJson, PushNotificationAction.OpenUrl::url, "url")
                    "OpenURLNotificationAction"
                }
                is PushNotificationAction.PresentWebsite -> {
                    putProp(this@encodeJson, PushNotificationAction.PresentWebsite::url, "url")
                    "PresentWebsiteNotificationAction"
                }
            }
        )
    }
}

internal fun PushNotification.Companion.decodeJson(json: JSONObject, dateFormatting: DateFormattingInterface): PushNotification {
    return PushNotification(
        id = json.getString("id"),
        title = json.getString("title"),
        body = json.getString("body"),
        channelId = json.optString("channelId", null),
        read = json.getBoolean("isRead"),
        deleted = json.getBoolean("isDeleted"),
        expiresAt = dateFormatting.iso8601AsDate(json.getString("expiresAt")),
        deliveredAt = dateFormatting.iso8601AsDate(json.getString("deliveredAt")),
        isNotificationCenterEnabled = json.getBoolean("isNotificationCenterEnabled"),
        action = PushNotificationAction.decodeJson(json.getJSONObject("action"))
    )
}
