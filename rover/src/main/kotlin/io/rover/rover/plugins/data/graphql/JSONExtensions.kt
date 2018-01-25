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
fun JSONObject.optIntOrNull(name: String): Int? {
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

fun <T, R> JSONObject.putProp(obj: T, prop: KProperty1<T, R>, transform: ((R) -> Any)? = null) {
    put(
        prop.name,
        if (transform != null) transform(prop.get(obj)) else prop.get(obj)
    )
}

/**
 * Get an [Iterable] over a [JSONArray], assuming/coercing all within to be strings.
 */
fun JSONArray.getStringIterable(): Iterable<String> = getIterable()

/**
 * Get an [Iterable] over a [JSONArray], assuming/coercing all within to be [JSONObject]s.
 */
fun JSONArray.getObjectIterable(): Iterable<JSONObject> = getIterable()

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.getIterable(): Iterable<T> {
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