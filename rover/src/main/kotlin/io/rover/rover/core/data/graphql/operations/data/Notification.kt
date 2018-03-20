package io.rover.rover.core.data.graphql.operations.data

import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.whenNotNull
import io.rover.rover.core.data.domain.PushNotificationAction
import io.rover.rover.core.data.domain.Notification
import io.rover.rover.core.data.domain.NotificationAttachment
import io.rover.rover.core.data.graphql.getDate
import io.rover.rover.core.data.graphql.putProp
import io.rover.rover.core.data.graphql.safeGetString
import io.rover.rover.core.data.graphql.safeOptDate
import io.rover.rover.core.data.graphql.safeOptString
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URL

internal fun Notification.encodeJson(dateFormatting: DateFormattingInterface): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Notification::id, "id")
        putProp(this@encodeJson, Notification::title, "title")
        putProp(this@encodeJson, Notification::body, "body")
        putProp(this@encodeJson, Notification::channelId, "channelId")
        putProp(this@encodeJson, Notification::isNotificationCenterEnabled, "isNotificationCenterEnabled")
        putProp(this@encodeJson, Notification::isRead, "isRead")
        putProp(this@encodeJson, Notification::isDeleted, "isDeleted")
        putProp(this@encodeJson, Notification::deliveredAt, "deliveredAt") { dateFormatting.dateAsIso8601(it )}
        putProp(this@encodeJson, Notification::expiresAt, "expiresAt") { it.whenNotNull { dateFormatting.dateAsIso8601(it) } }
        putProp(this@encodeJson, Notification::action, "action" ) {
            it.encodeJson()
        }
        putProp(this@encodeJson, Notification::attachment, "attachment") { it.whenNotNull { it.encodeJson() }}
    }
}

internal fun PushNotificationAction.Companion.decodeJson(json: JSONObject): PushNotificationAction {
    return PushNotificationAction(
        uri = URI(json.safeGetString("url"))
    )
}

internal fun NotificationAttachment.Companion.decodeJson(json: JSONObject): NotificationAttachment {
    // all three types have URLs.
    val type = json.safeGetString("type")
    val url = URL(json.safeGetString("url"))
    return when(type) {
        "AUDIO" -> NotificationAttachment.Audio(url)
        "IMAGE" -> NotificationAttachment.Image(url)
        "VIDEO" -> NotificationAttachment.Video(url)
        else -> throw JSONException("Unsupported Rover attachment type: $type")
    }
}

internal fun PushNotificationAction.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, PushNotificationAction::uri, "url")
    }
}

internal fun NotificationAttachment.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, NotificationAttachment::url, "url")
        put(
            "type",
            when(this@encodeJson) {
                is NotificationAttachment.Video -> "VIDEO"
                is NotificationAttachment.Audio -> "AUDIO"
                is NotificationAttachment.Image -> "IMAGE"
            }
        )
    }
}

internal fun Notification.Companion.decodeJson(json: JSONObject, dateFormatting: DateFormattingInterface): Notification {
    return Notification(
        id = json.safeGetString("id"),
        title = json.safeOptString("title"),
        body = json.safeGetString("body"),
        channelId = json.safeOptString("channelId"),
        isRead = json.getBoolean("isRead"),
        isDeleted = json.getBoolean("isDeleted"),
        expiresAt = json.safeOptDate("expiresAt", dateFormatting),
        deliveredAt = json.getDate("deliveredAt", dateFormatting),
        isNotificationCenterEnabled = json.getBoolean("isNotificationCenterEnabled"),
        action = PushNotificationAction.decodeJson(json.getJSONObject("action")),
        attachment = if (json.has("attachment") && !json.isNull("attachment")) NotificationAttachment.decodeJson(json.getJSONObject("attachment")) else null
    )
}
