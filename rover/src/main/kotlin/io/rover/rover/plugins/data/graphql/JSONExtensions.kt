@file:JvmName("JsonExtensions")

package io.rover.rover.plugins.data.graphql

import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KProperty1

/**
 * The standard [JSONObject.optInt] method does not support optional null values;
 * instead you must give a default int value.
 *
 * This method returns an optional Kotlin boxed [Int] value.
 */
internal fun JSONObject.optIntOrNull(name: String): Int? {
    val value = opt(name)
    return when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> {
            try {
                java.lang.Double.parseDouble(value).toInt()
            } catch (ignored: NumberFormatException) {
                null
            }
        }
        else -> null
    }
}

/**
 * The stock [JSONObject.optString] method has a nasty known bug for which the behaviour is kept
 * for backwards bug compatibility: if a `null` literal appears as the value for the string, you'll
 * get the string "null" back.
 *
 * This version of the method solves that problem.
 *
 * See [Android Bug #36924550](https://issuetracker.google.com/issues/36924550).
 */
internal fun JSONObject.safeOptString(name: String): String? {
    return if(isNull(name)) null else optString(name, null)
}

@Deprecated("This method uses reflection to obtain the property name, which is not appropriate in case of customer use of Proguard.", ReplaceWith("putProp(obj, prop, name, transform)"))
internal fun <T, R> JSONObject.putProp(obj: T, prop: KProperty1<T, R>, transform: ((R) -> Any?)? = null) {
    put(
        prop.name,
        if (transform != null) transform(prop.get(obj)) else prop.get(obj)
    )
}

internal fun <T, R> JSONObject.putProp(obj: T, prop: KProperty1<T, R>, name: String, transform: ((R) -> Any?)? = null) {
    put(
        name,
        if (transform != null) transform(prop.get(obj)) else prop.get(obj)
    )
}

/**
 * Get an [Iterable] over a [JSONArray], assuming/coercing all within to be strings.
 */
internal fun JSONArray.getStringIterable(): Iterable<String> = getIterable()

/**
 * Get an [Iterable] over a [JSONArray], assuming/coercing all within to be [JSONObject]s.
 */
fun JSONArray.getObjectIterable(): Iterable<JSONObject> = getIterable()

@Suppress("UNCHECKED_CAST")
internal fun <T> JSONArray.getIterable(): Iterable<T> {
    return object : Iterable<T> {
        private var counter = 0
        override fun iterator(): Iterator<T> {
            return object : Iterator<T> {
                override fun hasNext(): Boolean = counter < this@getIterable.length()

                override fun next(): T {
                    if (counter >= this@getIterable.length()) {
                        throw Exception("Iterator ran past the end!")
                    }
                    val jsonObject = this@getIterable.get(counter)
                    counter++
                    return jsonObject as T
                }
            }
        }
    }
}