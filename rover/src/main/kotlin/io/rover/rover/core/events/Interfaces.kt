package io.rover.rover.core.events

import io.rover.rover.core.data.domain.Context
import io.rover.rover.core.events.domain.Event

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

    /**
     * Called when this Context Provider is registered with the events plugin.
     */
    fun registeredWithEventQueue(eventQueue: EventQueueServiceInterface) { }
}

/**
 * This listens for a push token and ensures that it is transmitted up to the Rover API.
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
interface EventQueueServiceInterface {
    /**
     * Track the given [Event].  Enqueues it to be sent up to the Rover API.
     *
     * Asynchronous, will immediately return.
     *
     * @param namespace Specify a namespace name to your events in order to separate them from other
     * events. They will appear as a separate table in your BigQuery instance. May be left null to
     * have it appear in a default table.
     */
    fun trackEvent(event: Event, namespace: String? = null)

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
    @Deprecated("Remove this method once PushTokenTransmissionChannel can be directly injected to the NotificationHandler.")
    fun setPushToken(token: String?)
}
