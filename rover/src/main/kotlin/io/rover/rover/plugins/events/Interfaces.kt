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

    val contextProviders: List<ContextProvider>
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
     *
     * Typically will return immediately, but in some cases where a manual push token reset is
     * required it will block while doing some IO.  As such, do not use on the main thread.
     */
    fun captureContext(context: Context): Context
}

/**
 * This listens for push tokens and ensures it is transmitted up to the Rover API.
 */
interface PushTokenTransmissionChannel {
    /**
     * Set the given token as the token that should be sent up to the API, at soonest convenience.
     *
     * Stateful; will remember the given token throughout the installation lifetime of the app until
     * it is reset.
     *
     * Asynchronous; will return immediately.
     */
    fun setPushToken(token: String?)
}

/**
 * The events plugin is responsible for delivering [Event]s to the Rover cloud API.
 */
interface EventsPluginInterface {
    /**
     * Track the given [Event].  Enqueues it to be sent up to the Rover API.
     *
     * Asynchronous, will immediately return.
     */
    fun trackEvent(event: Event)

    /**
     * Install the given context provider, so that all outgoing events can the given context
     * provider populate some of the fields in a [Context].
     */
    fun addContextProvider(contextProvider: ContextProvider)

    /**
     * Enqueues an operation to flush any outstanding events to be executed immediately.
     *
     * Asynchronous, will immediately return.
     */
    fun flushNow()

    /**
     * A temporary method to allow the Push Plugin to set the outgoing firebase push token.
     *
     * Will be removed.
     */
    @Deprecated("Remove this method once PushTokenTransmissionChannel can be directly injected to the PushPlugin.")
    fun setPushToken(token: String?)
}
