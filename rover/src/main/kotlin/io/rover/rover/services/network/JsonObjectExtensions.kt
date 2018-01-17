@file:JvmName("JsonObjectExtensions")

package io.rover.rover.services.network

import org.json.JSONObject
import kotlin.reflect.KProperty1

fun <T, R> JSONObject.putProp(obj: T, prop: KProperty1<T, R>, transform: ((R) -> Any)? = null) {
    put(
        prop.name,
        if (transform != null) transform(prop.get(obj)) else prop.get(obj)
    )
}
