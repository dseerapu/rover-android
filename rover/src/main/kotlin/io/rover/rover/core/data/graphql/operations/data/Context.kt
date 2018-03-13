package io.rover.rover.core.data.graphql.operations.data

import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.data.graphql.putProp
import io.rover.rover.core.data.graphql.safeOptBoolean
import io.rover.rover.core.data.graphql.safeOptInt
import io.rover.rover.core.data.graphql.safeOptString
import org.json.JSONObject

/**
 * Outgoing JSON DTO transformation for [Context]s, as submitted to the Rover GraphQL API.
 */
internal fun Context.asJson(): JSONObject {
    return JSONObject().apply {
        val props = listOf(
            Context::appBuild,
            Context::appName,
            Context::appNamespace,
            Context::appVersion,
            Context::carrierName,
            Context::deviceManufacturer,
            Context::deviceModel,
            Context::isCellularEnabled,
            Context::isLocationServicesEnabled,
            Context::isWifiEnabled,
            Context::locationAuthorization,
            Context::localeLanguage,
            Context::localeRegion,
            Context::localeScript,
            Context::notificationAuthorization,
            Context::operatingSystemName,
            Context::operatingSystemVersion,
            Context::pushEnvironment,
            Context::pushToken,
            Context::radio,
            Context::screenWidth,
            Context::screenHeight,
            Context::timeZone
        )

        props.forEach { putProp(this@asJson, it) }

        putProp(this@asJson, Context::frameworks, "frameworks") { JSONObject(this@asJson.frameworks) }
    }
}

/**
 * Incoming JSON DTO transformation for [Context]s, as received from the Rover GraphQL API.
 */
internal fun Context.Companion.decodeJson(json: JSONObject): Context {
    return Context(
        appBuild = json.safeOptString("appBuild"),
        appName = json.safeOptString("appName"),
        appNamespace = json.safeOptString("appNamespace"),
        appVersion = json.safeOptString("appVersion"),
        carrierName = json.safeOptString("carrierName"),
        deviceManufacturer = json.safeOptString("deviceManufacturer"),
        deviceModel = json.safeOptString("deviceModel"),
        isCellularEnabled = json.safeOptBoolean("isCellularEnabled"),
        isLocationServicesEnabled = json.safeOptBoolean("isLocationServicesEnabled"),
        isWifiEnabled = json.safeOptBoolean("isWifiEnabled"),
        locationAuthorization = json.safeOptString("locationAuthorization"),
        localeLanguage = json.safeOptString("localeLanguage"),
        localeRegion = json.safeOptString("localeRegion"),
        localeScript = json.safeOptString("localeScript"),
        notificationAuthorization = json.safeOptString("notificationAuthorization"),
        operatingSystemName = json.safeOptString("operatingSystemName"),
        operatingSystemVersion = json.safeOptString("operatingSystemVersion"),
        pushEnvironment = json.safeOptString("pushEnvironment"),
        pushToken = json.safeOptString("pushToken"),
        radio = json.safeOptString("radio"),
        screenWidth = json.safeOptInt("screenWidth"),
        screenHeight = json.safeOptInt("screenHeight"),
        frameworks = json.getJSONObject("frameworks").asStringHash(),
        timeZone = json.safeOptString("timeZone")
    )
}


private fun Map<String, String>.encodeJson(): JSONObject {
    return JSONObject(this)
}

private fun JSONObject.asStringHash(): Map<String, String> {
    return this.keys().asSequence().map { key ->
        Pair(key, this@asStringHash.getString(key))
    }.associate { it }
}