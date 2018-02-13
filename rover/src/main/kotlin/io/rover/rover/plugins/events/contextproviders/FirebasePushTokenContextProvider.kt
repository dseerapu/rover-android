package io.rover.rover.plugins.events.contextproviders

import android.os.Handler
import io.rover.rover.core.logging.log
import io.rover.rover.platform.LocalStorage
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider
import io.rover.rover.plugins.events.EventsPluginInterface
import io.rover.rover.plugins.events.PushTokenTransmissionChannel
import io.rover.rover.plugins.events.domain.Event
import java.util.Date
import java.util.concurrent.Executors

/**
 * Captures and adds the Firebase push token to [Context].  As a [PushTokenTransmissionChannel], it
 * expects to be informed of any changes to the push token.
 */
class FirebasePushTokenContextProvider(
    localStorage: LocalStorage,
    private val resetPushToken: () -> Unit
): ContextProvider, PushTokenTransmissionChannel {

    private var eventsPlugin: EventsPluginInterface? = null
    override fun registeredWithEventsPlugin(eventsPlugin: EventsPluginInterface) {
        this.eventsPlugin = eventsPlugin
    }

    override fun captureContext(context: Context): Context {

        // problems:
        // - we only receive the callback when the key changes, and only after some delay.
        // - if FCM registration has already occurred before adding Rover 2.0, then we'll "never" get it.
        // - manually blocking until key is available does not work either; best you can do is at least trigger a refresh, but then wait for the callback.

        return context.copy(
            pushToken = token
        )
    }

    override fun setPushToken(token: String?) {
        if(this.token != token) {
            // if the new token is different, set it and emit Push Token Changed.
            this.token = token
            val eventsPlugin = eventsPlugin ?: throw RuntimeException("registeredWithEventsPlugin() not called on FirebasePushTokenContextProvider during setup.")
            eventsPlugin.trackEvent(
                Event(
                    "Push Token Changed",
                    hashMapOf()
                )
            )
            val elapsed = (Date().time - launchTime.time) / 1000
            log.v("Push token set after $elapsed seconds.")
        }

        this.token = token
    }

    private val launchTime = Date()
    private val keyValueStorage = localStorage.getKeyValueStorageFor(Companion.STORAGE_CONTEXT_IDENTIFIER)

    private var token: String?
        get() = keyValueStorage[Companion.TOKEN_KEY]
        set(token) { keyValueStorage[Companion.TOKEN_KEY] = token }

    init {
        if (token == null) {
            log.e("No push token is set yet.")
            Handler().postDelayed({
                if(token == null) {
                    // token still null? then attempt a reset. This case can happen if the FCM token
                    // was already set and received before the Rover SDK 2.x was integrated, meaning
                    // that FCM believes that the app knows what the push token is, but at least the
                    // Rover SDK itself does not.

                    log.w("Token is still not set.  Perhaps token was received before Rover SDK integrated.  Forcing reset.")
                    Executors.newSingleThreadExecutor().execute {
                        resetPushToken()
                    }
                }
            }, TOKEN_RESET_TIMEOUT)
        }
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.fcm-push-context-provider"
        private const val TOKEN_KEY = "push-token"
        private const val TOKEN_RESET_TIMEOUT = 4 * 1000L
    }
}
