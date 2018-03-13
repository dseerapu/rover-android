package io.rover.rover.core.data.graphql.operations.data

import io.rover.rover.core.data.domain.Profile
import io.rover.rover.core.data.graphql.putProp
import io.rover.rover.core.data.graphql.safeOptString
import org.json.JSONObject

internal fun Profile.Companion.decodeJson(jsonObject: JSONObject): Profile {
    return Profile(
        identifier = jsonObject.safeOptString("identifier"),
        attributes = jsonObject.getJSONObject("attributes").toFlatAttributesHash()
    )
}

internal fun Profile.encodeJson(): JSONObject {
    return JSONObject().apply {
        putProp(this@encodeJson, Profile::attributes) { it.encodeJson() }
        putProp(this@encodeJson, Profile::identifier)
    }
}
