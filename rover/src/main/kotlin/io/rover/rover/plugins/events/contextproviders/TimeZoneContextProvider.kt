package io.rover.rover.plugins.events.contextproviders

import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider
import java.util.TimeZone

/**
 * Captures and adds the device's time zone to a [Context].
 */
class TimeZoneContextProvider: ContextProvider {
    override fun captureContext(context: Context): Context {
        return context.copy(
            // Time zone name in Paul Eggert zoneinfo "America/Montreal" format.
            timeZone = TimeZone.getDefault().id
        )
    }
}