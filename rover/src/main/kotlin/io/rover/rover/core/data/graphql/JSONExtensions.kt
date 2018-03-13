@file:JvmName("JsonExtensions")

package io.rover.rover.core.data.graphql

import android.os.Build
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.whenNotNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.util.Date
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

/**
 * The stock [JSONObject.optBoolean] method cannot tell you if the value was unset or not present.
 */
internal fun JSONObject.safeOptBoolean(name: String): Boolean? {
    return if(isNull(name) || !this.has(name)) null else optBoolean(name)
}

internal fun JSONObject.safeOptInt(name: String): Int? {
    return if(isNull(name) || !this.has(name)) null else optInt(name)
}

internal fun JSONObject.getDate(name: String, dateFormatting: DateFormattingInterface, localTime : Boolean = false): Date {
    try {
        return dateFormatting.iso8601AsDate(getString(name), localTime)
    } catch (e: ParseException) {
        if (Build.VERSION.SDK_INT >= 27) {
            throw JSONException("Unable to parse date.", e)
        } else {
            throw JSONException("Unable to parse date, because: ${e.message}")
        }
    }
}

internal fun JSONObject.safeOptDate(name: String, dateFormatting: DateFormattingInterface, localTime : Boolean = false): Date? {
    try {
        return if (isNull(name) || !this.has(name)) null else optString(name, null).whenNotNull { dateFormatting.iso8601AsDate(it, localTime) }
    } catch (e: ParseException) {
        if (Build.VERSION.SDK_INT >= 27) {
            throw JSONException("Unable to parse date.", e)
        } else {
            throw JSONException("Unable to parse date, because: ${e.message}")
        }
    }
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