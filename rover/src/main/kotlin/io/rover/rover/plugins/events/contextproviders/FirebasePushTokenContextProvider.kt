package io.rover.rover.plugins.events.contextproviders

import io.rover.rover.core.logging.log
import io.rover.rover.platform.LocalStorage
import io.rover.rover.plugins.data.domain.Context
import io.rover.rover.plugins.events.ContextProvider
import io.rover.rover.plugins.events.PushTokenTransmissionChannel

/**
 * Captures and adds the Firebase push token to [Context].  As a [PushTokenTransmissionChannel], it
 * expects to be informed of any changes to the push token.
 */
class FirebasePushTokenContextProvider(
    localStorage: LocalStorage,
    private val synchronousResetAndAcquirePushToken: () -> String?
): ContextProvider, PushTokenTransmissionChannel {
    private val keyValueStorage = localStorage.getKeyValueStorageFor(Companion.STORAGE_CONTEXT_IDENTIFIER)


    override fun captureContext(context: Context): Context {
        val token = keyValueStorage[Companion.TOKEN_KEY] ?: synchronousResetAndAcquirePushToken().apply {
            log.w("Had to synchronously force refresh of Firebase Cloud Messaging push token.")
            setPushToken(this)
        }
        if(token == null) {
            log.e("A push token could not be acquired.  Is Firebase Cloud Messaging set up correctly?")
        }

        // this arrangement is a load of dingo's kidneys.

        // problems:
        // - we only receive the callback when the key changes, and only after some delay.
        // - if FCM registration has already occurred before adding Rover 2.0, then we'll "never" get it.
        // - manually blocking until key is available does not work either; best you can do is at least trigger a refresh, but then wait for the callback.


        // suggestion:  move the PushTokenContextProvider to happen *after* event has waited in the queue, and allow
        // it to fail the execution of the queue (ie., returning null instead of an augmented Context).

        return context.copy(
            pushToken = token
        )
    }

    override fun setPushToken(token: String?) {
        keyValueStorage[Companion.TOKEN_KEY] = token
        log.v("Push token set!")
    }

    companion object {
        private const val STORAGE_CONTEXT_IDENTIFIER = "io.rover.rover.fcm-push-context-provider"
        private const val TOKEN_KEY = "push-token"
    }
}
