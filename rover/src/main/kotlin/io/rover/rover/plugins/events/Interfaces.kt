package io.rover.rover.plugins.events

import android.app.Application
import io.rover.rover.platform.DateFormattingInterface
import io.rover.rover.platform.LocalStorage
import io.rover.rover.plugins.data.DataPluginInterface
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.domain.Event

interface EventsPluginComponentsInterface {
    val dataPlugin: DataPluginInterface

    val localStorage: LocalStorage

    val dateFormatting: DateFormattingInterface

    val application: Application
}

/**
 * Objects that can contribute to a [Context] structure.
 *
 * Not to be confused with an Android context.
 */
interface ContextProvider {
    /**
     * Returns a new [Context] with the fields from the provided original ([context]) and the relevant
     * fields that this context provider can set.
     */
    fun captureContext(context: Context): Context
}
/**
 * The events plugin is responsible for delivering [Event]s to the Rover cloud API.
 */
interface EventsPluginInterface {
    fun addContextProvider(contextProvider: ContextProvider)
    fun trackEvent(event: Event)
    fun flushNow()
}
