package io.rover.rover.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface DateFormattingInterface {
    fun dateAsIso8601(date: Date, localTime: Boolean = false): String

    fun iso8601AsDate(iso8601Date: String, localTime: Boolean = false): Date
}

class DateFormatting : DateFormattingInterface {

    private val format8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val format8601WithTimeZone = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    override fun dateAsIso8601(date: Date, localTime: Boolean): String = if(localTime) format8601WithTimeZone.format(date) else format8601.format(date)

    override fun iso8601AsDate(iso8601Date: String, localTime: Boolean): Date = if(localTime) format8601WithTimeZone.parse(iso8601Date) else format8601.parse(iso8601Date)
}
